package com.ursolgleb.controlparental.data.remote.models

/**
 * Respuesta del endpoint POST /api/devices/{deviceId}/heartbeat
 * Ejemplo JSON:
 * {
 *   "success": true,
 *   "status": "online",
 *   "server_time": "2025-01-16T10:30:00.000Z",
 *   "next_ping_seconds": 30
 * }
 */
data class HeartbeatResponse(
    val success: Boolean,
    val status: String,
    val server_time: String,
    val next_ping_seconds: Int
) 