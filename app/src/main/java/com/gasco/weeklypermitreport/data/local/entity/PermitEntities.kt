package com.gasco.weeklypermitreport.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class PermitCategory(val sectionTitle: String) {
    PM("Preventive Maintenance (PM) Activities"),
    CM("Corrective Maintenance (CM) Activities"),
    COMM("Communication (COMM) Activities"),
    ENG("Engineering (ENG) Activities")
}

@Entity(tableName = "permit_reports")
data class PermitReportEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "report_period")
    val reportPeriod: String,
    @ColumnInfo(name = "site_name")
    val siteName: String = "Amerya LPG Plant",
    val department: String = "Instrumentation & Control",
    @ColumnInfo(name = "additional_notes")
    val additionalNotes: String = "",
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "permit_activities",
    foreignKeys = [
        ForeignKey(
            entity = PermitReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["report_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["report_id"]),
        Index(value = ["category"]),
        Index(value = ["date_iso"])
    ]
)
data class PermitActivityEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "report_id")
    val reportId: String,
    val category: PermitCategory = PermitCategory.PM,
    @ColumnInfo(name = "date_iso")
    val dateIso: String,
    @ColumnInfo(name = "display_date")
    val displayDate: String,
    val equipment: String,
    val activity: String,
    @ColumnInfo(name = "permit_number")
    val permitNumber: String? = null,
    @ColumnInfo(name = "photo_uri")
    val photoUri: String? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
