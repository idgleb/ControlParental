package com.ursolgleb.controlparental.data.apps

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ursolgleb.controlparental.data.apps.dao.AppDao
import com.ursolgleb.controlparental.data.apps.dao.HorarioDao
import com.ursolgleb.controlparental.data.apps.dao.UsageEventDao
import com.ursolgleb.controlparental.data.apps.dao.UsageLimitDao
import com.ursolgleb.controlparental.data.apps.dao.UsageStatsDao
import com.ursolgleb.controlparental.data.apps.entities.AppEntity
import com.ursolgleb.controlparental.data.apps.entities.HorarioEntity
import com.ursolgleb.controlparental.data.apps.entities.UsageEventEntity
import com.ursolgleb.controlparental.data.apps.entities.UsageLimitEntity
import com.ursolgleb.controlparental.data.apps.entities.UsageStatsEntity
import com.ursolgleb.controlparental.utils.Converters

@Database(
    entities = [
        AppEntity::class,
        HorarioEntity::class,
        UsageLimitEntity::class,
        UsageEventEntity::class,
        UsageStatsEntity::class],
    version = 18,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun horarioDao(): HorarioDao
    abstract fun usageEventDao(): UsageEventDao
    abstract fun usageStatsDao(): UsageStatsDao
    abstract fun usageLimitDao(): UsageLimitDao
}
