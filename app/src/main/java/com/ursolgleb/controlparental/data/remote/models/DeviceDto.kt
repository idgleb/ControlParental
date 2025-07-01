package com.ursolgleb.controlparental.data.remote.models

/**
 * DTO utilizado para sincronizar información del dispositivo con el servidor
 * Usado en endpoints:
 * - GET /api/sync/devices?deviceId={deviceId} (devuelve List<DeviceDto>)
 * - POST /api/sync/devices (un solo DeviceDto)
 * - POST /api/sync/events (dentro del campo "data" de EventDto cuando entity_type="device")
 * Ejemplo JSON:
 * {
 *   "deviceId": "abc-123-def-456",
 *   "model": "Samsung Galaxy S21",
 *   "batteryLevel": 85
 * }
 * 
 * Notas:
 * - deviceId: Identificador único del dispositivo (generalmente Android ID)
 * - model: Modelo del dispositivo obtenido de Build.MODEL
 * - batteryLevel: Nivel de batería en porcentaje (0-100)
 */
data class DeviceDto(
    val deviceId: String?,     // ID único del dispositivo
    val model: String?,        // Modelo del dispositivo
    val batteryLevel: Int?     // Nivel de batería (0-100)
)