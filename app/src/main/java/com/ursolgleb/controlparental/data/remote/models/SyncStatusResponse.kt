package com.ursolgleb.controlparental.data.remote.models

/**
 * Respuesta del endpoint /sync/status
 * Ejemplo JSON:
 * {
 *   "status": "success",
 *   "deviceId": "abc",
 *   "pendingEvents": {"horario": 3},
 *   "lastEventId": 42,
 *   "lastEventTime": "2025-06-29T05:25:26+00:00",
 *   "serverTime": "2025-06-29T05:25:26+00:00"
 * }
 */
data class SyncStatusResponse(
    val status: String,
    val deviceId: String,
    val pendingEvents: Map<String, Int> = emptyMap(),
    val lastEventId: Long,
    val lastEventTime: String? = null,
    val serverTime: String
) 