package com.ursolgleb.controlparental.data.apps.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val deviceId: String,
    val model: String,
    val batteryLevel: Int
)