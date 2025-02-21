package com.ursolgleb.controlparental.data.local.dao

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

    @Query("SELECT * FROM apps ORDER BY tiempoUsoSegundosHoy DESC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE blocked = 1 ORDER BY tiempoUsoSegundosHoy DESC")
    fun getAllAppsBlocked(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE blocked = 0 ORDER BY tiempoUsoSegundosHoy DESC")
    fun getAllAppsMenosBlocked(): Flow<List<AppEntity>>

    // 🔄 Cambiar todas las apps bloqueadas a desbloqueadas
    @Query("UPDATE apps SET blocked = 0 WHERE blocked = 1")
    suspend fun unblockAllApps()

    @Update
    suspend fun updateApp(app: AppEntity)

    @Query("DELETE FROM apps")
    suspend fun deleteAllApps()

}
