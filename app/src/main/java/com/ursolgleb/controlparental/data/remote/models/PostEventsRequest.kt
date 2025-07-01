package com.ursolgleb.controlparental.data.remote.models

/**
 * Request body para el endpoint POST /api/sync/events
 * Endpoint: POST /api/sync/events
 * Ejemplo JSON:
 * {
 *   "deviceId": "abc-123-def-456",
 *   "events": [
 *     {
 *       "entity_type": "horario",
 *       "entity_id": "123",
 *       "action": "update",
 *       "data": {
 *         "nombreDeHorario": "Horario Escolar",
 *         "horaInicio": "08:00",
 *         "horaFin": "14:00"
 *       },
 *       "timestamp": "2025-06-29T10:30:00Z"
 *     },
 *     {
 *       "entity_type": "app",
 *       "entity_id": "com.example.game",
 *       "action": "delete",
 *       "timestamp": "2025-06-29T10:31:00Z"
 *     }
 *   ]
 * }
 */
data class PostEventsRequest(
    val deviceId: String,
    val events: List<EventDto>
)