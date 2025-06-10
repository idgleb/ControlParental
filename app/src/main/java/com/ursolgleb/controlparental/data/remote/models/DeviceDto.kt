package com.ursolgleb.controlparental.data.remote.models

/**
 * DTO utilizado para sincronizar información del dispositivo con el servidor.
 */
data class DeviceDto(
    val deviceId: String?,
    val model: String?,
    val batteryLevel: Int?
)