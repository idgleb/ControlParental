package com.ursolgleb.controlparental.data.apps.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ursolgleb.controlparental.data.apps.entities.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {

    @Query("SELECT COUNT(*) FROM apps WHERE packageName = :pkg LIMIT 1")
    suspend fun countByPackage(pkg: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListaApps(apps: List<AppEntity>)

    @Query("SELECT * FROM apps ORDER BY usageTimeToday DESC")
    fun getAllApps(): Flow<List<AppEntity>>

    //  Cambiar todas las apps bloqueadas a DISPONIBLE
    @Query("UPDATE apps SET appStatus = 'DISPONIBLE' WHERE appStatus = 'BLOQUEADA'")
    suspend fun unblockAllApps()

    @Query("DELETE FROM apps")
    suspend fun deleteAllApps()

    @Query(
        """
    UPDATE apps 
    SET usageTimeToday = :hoy, timeStempUsageTimeToday = strftime('%s','now')
    WHERE packageName = :packageName
"""
    )
    suspend fun updateUsageTimesForAppHoy(packageName: String, hoy: Long)


    suspend fun updateUsageTimeHoy(usageMap: MutableMap<String, Long>) {

        usageMap.forEach { (packageName, usageTimesHoy) ->
            updateUsageTimesForAppHoy(packageName, usageTimesHoy)
        }

    }

}
