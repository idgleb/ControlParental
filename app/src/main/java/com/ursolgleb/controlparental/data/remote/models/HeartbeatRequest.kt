package com.ursolgleb.controlparental.data.remote.models

/**
 * Request para el endpoint POST /api/devices/{deviceId}/heartbeat
 */
data class HeartbeatRequest(
    val latitude: Double? = null,
    val longitude: Double? = null
) 