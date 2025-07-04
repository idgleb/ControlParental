package com.ursolgleb.controlparental.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ursolgleb.controlparental.data.local.dao.AppDao
import com.ursolgleb.controlparental.data.local.dao.HorarioDao
import com.ursolgleb.controlparental.data.local.dao.UsageEventDao
import com.ursolgleb.controlparental.data.local.dao.UsageStatsDao
import com.ursolgleb.controlparental.data.local.dao.DeviceDao
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.HorarioEntity
import com.ursolgleb.controlparental.data.local.entities.UsageEventEntity
import com.ursolgleb.controlparental.data.local.entities.UsageStatsEntity
import com.ursolgleb.controlparental.data.local.entities.DeviceEntity
import com.ursolgleb.controlparental.utils.Converters

@Database(
    entities = [
        AppEntity::class,
        HorarioEntity::class,
        UsageEventEntity::class,
        UsageStatsEntity::class,
        DeviceEntity::class],
    version = 53,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun horarioDao(): HorarioDao
    abstract fun usageEventDao(): UsageEventDao
    abstract fun usageStatsDao(): UsageStatsDao
    abstract fun deviceDao(): DeviceDao
}
