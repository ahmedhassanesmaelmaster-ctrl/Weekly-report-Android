package com.gasco.weeklypermitreport.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.gasco.weeklypermitreport.data.local.dao.PermitActivityDao
import com.gasco.weeklypermitreport.data.local.dao.PermitReportDao
import com.gasco.weeklypermitreport.data.local.entity.PermitActivityEntity
import com.gasco.weeklypermitreport.data.local.entity.PermitCategory
import com.gasco.weeklypermitreport.data.local.entity.PermitReportEntity

class PermitConverters {
    @TypeConverter
    fun fromCategory(value: PermitCategory): String = value.name

    @TypeConverter
    fun toCategory(value: String): PermitCategory = PermitCategory.valueOf(value)
}

@Database(
    entities = [PermitReportEntity::class, PermitActivityEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(PermitConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): PermitReportDao
    abstract fun activityDao(): PermitActivityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gasco_permit_report.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
