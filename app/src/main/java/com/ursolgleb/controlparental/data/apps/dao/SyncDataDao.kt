package com.ursolgleb.controlparental.data.apps.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ursolgleb.controlparental.data.apps.entities.DeviceEntity
import com.ursolgleb.controlparental.data.apps.entities.SyncDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(syncData: SyncDataEntity)

    @Query("SELECT * FROM sync_data LIMIT 1")
    fun getSyncData(): Flow<SyncDataEntity?>

    @Query("SELECT * FROM sync_data LIMIT 1")
    suspend fun getSyncDataOnce(): SyncDataEntity?

    @Query("DELETE FROM sync_data")
    suspend fun deleteAll()

    @Transaction
    suspend fun replace(syncData: SyncDataEntity) {
        deleteAll()
        insert(syncData)
    }

}