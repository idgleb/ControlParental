package com.ursolgleb.controlparental.data.apps.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ursolgleb.controlparental.data.apps.entities.DeviceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(device: DeviceEntity): Long

    @Update
    suspend fun update(device: DeviceEntity)

    @Query("UPDATE apps SET deviceId = :newId WHERE deviceId = :oldId")
    suspend fun updateDeviceIdInApps(oldId: String, newId: String)

    @Query("UPDATE horarios SET deviceId = :newId WHERE deviceId = :oldId")
    suspend fun updateDeviceIdInHorarios(oldId: String, newId: String)

    @Query("UPDATE devices SET deviceId = :newId, model = :model, batteryLevel = :battery WHERE deviceId = :oldId")
    suspend fun updateDevice(oldId: String, newId: String, model: String, battery: Int)

    @Query("SELECT * FROM devices LIMIT 1")
    fun getDevice(): Flow<DeviceEntity?>

    @Query("SELECT * FROM devices LIMIT 1")
    suspend fun getDeviceOnce(): DeviceEntity?

    @Query("DELETE FROM devices")
    suspend fun deleteAll()

    @Transaction
    suspend fun replace(device: DeviceEntity) {
        val existing = getDeviceOnce()
        if (existing == null) {
            insertIgnore(device)
        } else {
            if (existing.deviceId != device.deviceId) {
                updateDeviceIdInHorarios(existing.deviceId, device.deviceId)
                updateDeviceIdInApps(existing.deviceId, device.deviceId)
                updateDevice(existing.deviceId, device.deviceId, device.model, device.batteryLevel)
            } else {
                update(device)
            }
        }
    }

}