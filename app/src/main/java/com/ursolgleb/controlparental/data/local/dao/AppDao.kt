package com.ursolgleb.controlparental.data.local.dao

import android.util.Log
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.BlockedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListaApps(apps: List<AppEntity>)

    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getApp(packageName: String): AppEntity?

    @Query("SELECT * FROM apps ORDER BY tiempoUsoHoy DESC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE blocked = 1 ORDER BY tiempoUsoHoy DESC")
    fun getAllAppsBlocked(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE blocked = 0 ORDER BY tiempoUsoHoy DESC")
    fun getAllAppsMenosBlocked(): Flow<List<AppEntity>>

    // 🔄 Cambiar todas las apps bloqueadas a desbloqueadas
    @Query("UPDATE apps SET blocked = 0 WHERE blocked = 1")
    suspend fun unblockAllApps()

    @Update
    suspend fun updateApp(app: AppEntity)

    @Query("DELETE FROM apps")
    suspend fun deleteAllApps()

    @Query(
        """
    UPDATE apps 
    SET tiempoUsoHoy = :hoy, timeStempToday = strftime('%s','now'),
        timeUsoMes = :mes, timeStempMes = strftime('%s','now')
    WHERE packageName = :packageName
"""
    )
    suspend fun updateUsageTimesForApp(packageName: String, hoy: Long, mes: Map<Int, Long>)


    suspend fun updateUsageTimes(usageMap: MutableMap<String, MutableList<Long>>) {
        val usageData: Map<Int, Long> = mapOf(
            1 to 7200000,   // Día 1: 2 horas
            2 to 5400000,   // Día 2: 1.5 horas
            3 to 0,         // Día 3: 0 horas
            // Puedes añadir más días según sea necesario
        )
        usageMap.forEach { (packageName, usageTimes) ->
            if (usageTimes.size == 3) {
                updateUsageTimesForApp(packageName, usageTimes[0], usageData)
            } else {
                // Manejar el caso en que la lista no tiene el tamaño esperado (3)
                Log.e(
                    "AppDao",
                    "Lista de tiempos de uso incorrecta para $packageName: ${usageTimes.size} elementos"
                )
            }
        }
    }

}
