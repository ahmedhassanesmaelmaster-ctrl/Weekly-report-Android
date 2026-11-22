package com.gasco.weeklypermitreport.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.gasco.weeklypermitreport.data.local.entity.PermitActivityEntity
import com.gasco.weeklypermitreport.data.local.entity.PermitReportEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PermitReportDao {
    @Query("SELECT * FROM permit_reports ORDER BY updated_at DESC")
    fun observeAllReports(): Flow<List<PermitReportEntity>>

    @Query("SELECT * FROM permit_reports WHERE id = :reportId LIMIT 1")
    fun observeReport(reportId: String): Flow<PermitReportEntity?>

    @Upsert
    suspend fun upsertReport(report: PermitReportEntity)

    @Delete
    suspend fun deleteReport(report: PermitReportEntity)
}

@Dao
interface PermitActivityDao {
    @Query(
        """
        SELECT * FROM permit_activities
        WHERE report_id = :reportId
        ORDER BY date_iso ASC, created_at ASC
        """
    )
    fun observeActivitiesForReport(reportId: String): Flow<List<PermitActivityEntity>>

    @Upsert
    suspend fun upsertActivity(activity: PermitActivityEntity)

    @Upsert
    suspend fun upsertActivities(activities: List<PermitActivityEntity>)

    @Delete
    suspend fun deleteActivity(activity: PermitActivityEntity)
}
