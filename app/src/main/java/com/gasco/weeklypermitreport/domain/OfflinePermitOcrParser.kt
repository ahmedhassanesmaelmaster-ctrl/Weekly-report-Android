package com.gasco.weeklypermitreport.domain

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.gasco.weeklypermitreport.data.local.entity.PermitActivityEntity
import com.gasco.weeklypermitreport.data.local.entity.PermitCategory
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object OfflinePermitOcrParser {
    private val dateRegex = Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](20\d{2})\b""")
    private val permitNoRegex = Regex("""\b(PTW|WP|MOC|WO)[\s#-]*[A-Z0-9-]{3,14}\b""", RegexOption.IGNORE_CASE)
    private val tagRegex = Regex("""\b(\d{2,3})?(PT|TT|LT|FT|FV|PCV|TCV|PI|PDSH|XV|PSV|GD|Z\d+L|Fan\s+\d+-[A-Z]-\d+)\w*\b""", RegexOption.IGNORE_CASE)

    suspend fun extractFromPhoto(
        context: Context,
        reportId: String,
        photoUri: Uri,
        additionalNotes: String
    ): PermitActivityEntity {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val inputImage = InputImage.fromFilePath(context, photoUri)
        val recognizedText = recognizer.process(inputImage).await().text
        return parseRawText(reportId, recognizedText, additionalNotes, photoUri.toString())
    }

    fun parseRawText(
        reportId: String,
        rawText: String,
        additionalNotes: String,
        photoUri: String? = null
    ): PermitActivityEntity {
        val combined = "$rawText\n$additionalNotes"
        val lower = combined.lowercase()

        val category = when {
            lower.contains("comm") || lower.contains("telephone") || lower.contains("intercom") || lower.contains("radio") || lower.contains("cable") -> PermitCategory.COMM
            lower.contains("eng") || lower.contains("moc") || lower.contains("modification") || lower.contains("logic") -> PermitCategory.ENG
            lower.contains("cm") || lower.contains("corrective") || lower.contains("leak") || lower.contains("fault") || lower.contains("replace") || lower.contains("loose") -> PermitCategory.CM
            else -> PermitCategory.PM
        }

        val today = LocalDate.now()
        val dateMatch = dateRegex.find(rawText)
        val (dateIso, displayDate) = if (dateMatch != null) {
            val d = dateMatch.groupValues[1].padStart(2, '0')
            val m = dateMatch.groupValues[2].padStart(2, '0')
            val y = dateMatch.groupValues[3]
            "$y-$m-$d" to "$d/$m/$y"
        } else {
            today.toString() to today.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        }

        val permitNo = permitNoRegex.find(rawText)?.value?.uppercase()?.replace(" ", "-")
        val equipment = tagRegex.findAll(rawText).map { it.value.trim() }.distinct().take(3).joinToString(", ")
            .ifBlank { "Plant Instrument" }

        val cleanedDesc = rawText
            .lines()
            .map { it.trim() }
            .filter { it.length > 8 }
            .take(2)
            .joinToString(" ")
            .ifBlank { "Maintenance and calibration." }
            .let { if (it.endsWith(".")) it else "$it." }

        return PermitActivityEntity(
            reportId = reportId,
            category = category,
            dateIso = dateIso,
            displayDate = displayDate,
            equipment = equipment,
            activity = cleanedDesc.take(220),
            permitNumber = permitNo,
            photoUri = photoUri
        )
    }
}
