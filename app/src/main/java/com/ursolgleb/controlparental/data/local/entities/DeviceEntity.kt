package com.ursolgleb.controlparental.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class DeviceEntity(
    @PrimaryKey val deviceId: String,
    val model: String,
    val batteryLevel: Int,
    
    // Campos de ubicación
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationUpdatedAt: Long? = null,
    
    // Campos para heartbeat y sincronización
    val lastSeen: Long = System.currentTimeMillis(),
    val pingIntervalSeconds: Int = 30,
    
    // Timestamps
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)