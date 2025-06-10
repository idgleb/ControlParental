package com.ursolgleb.controlparental.data.apps.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ursolgleb.controlparental.data.apps.entities.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(device: DeviceEntity)

    @Query("SELECT * FROM devices LIMIT 1")
    fun getDevice(): Flow<DeviceEntity?>

    @Query("SELECT * FROM devices LIMIT 1")
    suspend fun getDeviceOnce(): DeviceEntity?

    @Query("DELETE FROM devices")
    suspend fun deleteAll()


}