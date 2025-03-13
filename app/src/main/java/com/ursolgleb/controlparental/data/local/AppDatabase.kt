package com.ursolgleb.controlparental.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ursolgleb.controlparental.Converters
import com.ursolgleb.controlparental.data.local.dao.AppDao
import com.ursolgleb.controlparental.data.local.dao.BlockedDao
import com.ursolgleb.controlparental.data.local.dao.UsageEventDao
import com.ursolgleb.controlparental.data.local.dao.UsageLimitDao
import com.ursolgleb.controlparental.data.local.dao.UsageStatsDao
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.BlockedEntity
import com.ursolgleb.controlparental.data.local.entities.UsageEventEntity
import com.ursolgleb.controlparental.data.local.entities.UsageLimitEntity
import com.ursolgleb.controlparental.data.local.entities.UsageStatsEntity

@Database(
    entities = [AppEntity::class, BlockedEntity::class, UsageLimitEntity::class, UsageEventEntity::class, UsageStatsEntity::class],
    version = 11,  // Aumenta la versión (antes era 8)
    exportSchema = true
)
@TypeConverters(Converters::class)  // ✅ Registrar el TypeConverter
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun blockedDao(): BlockedDao
    abstract fun usageLimitDao(): UsageLimitDao
    abstract fun usageEventDao(): UsageEventDao // 🔹 Agregado
    abstract fun usageStatsDao(): UsageStatsDao // 🔹 Agregado
}
