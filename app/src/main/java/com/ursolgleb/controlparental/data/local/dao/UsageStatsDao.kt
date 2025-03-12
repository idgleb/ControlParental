package com.ursolgleb.controlparental.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ursolgleb.controlparental.data.local.entities.UsageStatsEntity

@Dao
interface UsageStatsDao {

    // 🔹 Insertar un nuevo registro de uso
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageStat(usageStat: UsageStatsEntity)

    // 🔹 Insertar múltiples registros
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUsageStats(stats: List<UsageStatsEntity>)

    // 🔹 Obtener todos los registros ordenados por fecha
    @Query("SELECT * FROM usage_stats ORDER BY dia DESC")
    suspend fun getAllUsageStats(): List<UsageStatsEntity>

    // 🔹 Obtener el uso de una app específica en un día específico
    @Query("SELECT * FROM usage_stats WHERE packageName = :packageName AND dia = :dia LIMIT 1")
    suspend fun getUsageByPackageAndDay(packageName: String, dia: Long): UsageStatsEntity?

    // 🔹 Obtener el uso total de una app en los últimos N días
    @Query("SELECT SUM(usageDuration) FROM usage_stats WHERE packageName = :packageName AND dia >= :startDay")
    suspend fun getTotalUsageForApp(packageName: String, startDay: Long): Long?

    // 🔹 Borrar registros antiguos (por ejemplo, mayores a 30 días)
    @Query("DELETE FROM usage_stats WHERE dia < :cutoffDay")
    suspend fun deleteOldUsageStats(cutoffDay: Long)

    @Query("SELECT MAX(dia) FROM usage_stats")
    suspend fun getLastDia(): Long?


}
