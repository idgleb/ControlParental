package com.ursolgleb.controlparental.data.remote.models

/**
 * Respuesta del endpoint GET /api/sync/events
 * Endpoint: GET /api/sync/events?deviceId={deviceId}&lastEventId={lastEventId}&types={types}
 * Ejemplo JSON:
 * {
 *   "status": "success",
 *   "events": [
 *     {
 *       "id": 42,
 *       "deviceId": "abc-123",
 *       "entity_type": "horario",
 *       "entity_id": "123",
 *       "action": "update",
 *       "data": {...},
 *       "created_at": "2025-06-29T10:30:00Z"
 *     },
 *     {
 *       "id": 43,
 *       "deviceId": "abc-123",
 *       "entity_type": "app",
 *       "entity_id": "com.example.app",
 *       "action": "create",
 *       "data": {...},
 *       "created_at": "2025-06-29T10:31:00Z"
 *     }
 *   ],
 *   "lastEventId": 43,
 *   "hasMore": true,
 *   "timestamp": "2025-06-29T10:35:00Z"
 * }
 */
data class SyncEventsResponse(
    val status: String,              // "success" o "error"
    val events: List<SyncEvent>,     // Lista de eventos pendientes
    val lastEventId: Long,           // ID del último evento en esta respuesta
    val hasMore: Boolean,            // Si hay más eventos disponibles
    val timestamp: String            // Timestamp del servidor
) 