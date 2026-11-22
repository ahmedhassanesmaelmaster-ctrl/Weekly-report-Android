package com.gasco.weeklypermitreport.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gasco.weeklypermitreport.data.local.AppDatabase
import com.gasco.weeklypermitreport.data.local.entity.PermitActivityEntity
import com.gasco.weeklypermitreport.data.local.entity.PermitCategory
import com.gasco.weeklypermitreport.data.local.entity.PermitReportEntity
import com.gasco.weeklypermitreport.domain.GascoExportEngine
import com.gasco.weeklypermitreport.domain.OfflinePermitOcrParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class PermitUiState(
    val reports: List<PermitReportEntity> = emptyList(),
    val activeReport: PermitReportEntity? = null,
    val activities: List<PermitActivityEntity> = emptyList(),
    val isScanningOcr: Boolean = false,
    val statusMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class PermitViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val reportDao = db.reportDao()
    private val activityDao = db.activityDao()

    private val activeReportId = MutableStateFlow<String?>(null)
    private val isScanning = MutableStateFlow(false)
    private val statusMsg = MutableStateFlow<String?>(null)

    private val allReports = reportDao.observeAllReports()
    private val currentActivities = activeReportId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else activityDao.observeActivitiesForReport(id)
    }

    val uiState: StateFlow<PermitUiState> = combine(
        allReports,
        activeReportId,
        currentActivities,
        isScanning,
        statusMsg
    ) { reports, selectedId, activities, scanning, message ->
        val active = reports.firstOrNull { it.id == selectedId } ?: reports.firstOrNull()
        if (active != null && selectedId == null) {
            activeReportId.value = active.id
        }
        PermitUiState(
            reports = reports,
            activeReport = active,
            activities = activities,
            isScanningOcr = scanning,
            statusMessage = message
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PermitUiState())

    fun defaultCurrentPeriod(): String {
        val fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
        val end = start.plusDays(3)
        return "${start.format(fmt)}-${end.format(fmt)}"
    }

    fun createOrUpdateReport(period: String, notes: String): String {
        val current = uiState.value.activeReport
        val report = if (current != null && current.reportPeriod == period.trim()) {
            current.copy(additionalNotes = notes.trim(), updatedAt = System.currentTimeMillis())
        } else {
            PermitReportEntity(
                reportPeriod = period.trim().ifBlank { defaultCurrentPeriod() },
                additionalNotes = notes.trim()
            )
        }
        viewModelScope.launch {
            reportDao.upsertReport(report)
            activeReportId.value = report.id
        }
        return report.id
    }

    fun importPermitPhotosOffline(period: String, notes: String, uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            isScanning.value = true
            statusMsg.value = "Running offline ML Kit OCR..."
            try {
                val reportId = createOrUpdateReport(period, notes)
                val app = getApplication<Application>()
                val extracted = uris.map { uri ->
                    OfflinePermitOcrParser.extractFromPhoto(app, reportId, uri, notes)
                }
                activityDao.upsertActivities(extracted)
                statusMsg.value = "Saved ${extracted.size} activities into local Room database"
            } catch (e: Exception) {
                statusMsg.value = "OCR error: ${e.message ?: "check image"}"
            } finally {
                isScanning.value = false
            }
        }
    }

    fun saveManualActivity(
        period: String,
        category: PermitCategory,
        displayDate: String,
        dateIso: String,
        equipment: String,
        activity: String
    ) {
        viewModelScope.launch {
            val reportId = uiState.value.activeReport?.id ?: createOrUpdateReport(period, "")
            val trimmed = activity.trim()
            activityDao.upsertActivity(
                PermitActivityEntity(
                    reportId = reportId,
                    category = category,
                    dateIso = dateIso,
                    displayDate = displayDate,
                    equipment = equipment.trim(),
                    activity = if (trimmed.endsWith(".")) trimmed else "$trimmed."
                )
            )
            statusMsg.value = "Saved to Room database"
        }
    }

    fun deleteActivity(activity: PermitActivityEntity) {
        viewModelScope.launch {
            activityDao.deleteActivity(activity)
        }
    }

    fun buildShareIntent(kind: String): Intent? {
        val state = uiState.value
        val report = state.activeReport ?: return null
        val app = getApplication<Application>()
        val (file: File, mime: String) = when (kind) {
            "pdf" -> GascoExportEngine.writeTwoPagePdf(app, report, state.activities) to "application/pdf"
            "png" -> GascoExportEngine.writeCoverPng(app) to "image/png"
            else -> GascoExportEngine.writeLatexFile(app, report, state.activities) to "application/x-tex"
        }
        val uri = FileProvider.getUriForFile(app, "${app.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "GASCO Weekly Activities Report (${report.reportPeriod})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
