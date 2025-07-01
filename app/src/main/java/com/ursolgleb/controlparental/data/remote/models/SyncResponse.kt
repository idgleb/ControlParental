package com.ursolgleb.controlparental.data.remote.models

/**
 * Respuesta genérica de sincronización
 * Usada principalmente por:
 * - GET /api/sync/horarios?deviceId={deviceId} (retorna Response<SyncResponse<HorarioDto>>)
 * Ejemplo JSON exitoso:
 * {
 *   "status": "success",
 *   "data": [...],
 *   "changes": {
 *     "added": [1, 2, 3],
 *     "updated": [4, 5],
 *     "deleted": [6]
 *   },
 *   "timestamp": "2025-06-29T10:30:00Z",
 *   "totalChanges": 6,
 *   "deviceId": "abc-123"
 * }
 * 
 * Ejemplo JSON error:
 * {
 *   "status": "error",
 *   "message": "Failed to fetch data",
 *   "error": "Database connection failed",
 *   "details": "Connection timeout after 30s"
 * }
 */
data class SyncResponse<T>(
    val status: String,
    val data: List<T>? = null,
    val changes: Changes? = null,
    val timestamp: String? = null,
    val totalChanges: Int? = null,
    val message: String? = null,
    val deviceId: String? = null,
    val error: String? = null,
    val details: String? = null
)