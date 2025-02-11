package com.ursolgleb.controlparental.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertApp(app: AppEntity)

    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    suspend fun getApp(packageName: String): AppEntity?

    @Query("SELECT * FROM apps")
    fun getAllApps(): Flow<List<AppEntity>>

    @Update
    suspend fun updateApp(app: AppEntity)

    @Query("DELETE FROM apps")  // Reemplaza "apps_table" con el nombre de tu tabla.
    suspend fun deleteAllApps()

}
