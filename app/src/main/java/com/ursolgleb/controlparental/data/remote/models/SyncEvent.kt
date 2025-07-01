package com.ursolgleb.controlparental.data.remote.models

/**
 * Evento de sincronización almacenado en el servidor
 * Recibido desde endpoint: GET /api/sync/events (dentro del array "events" de SyncEventsResponse)
 * Ejemplo JSON:
 * {
 *   "id": 42,
 *   "deviceId": "abc-123-def-456",
 *   "entity_type": "horario",
 *   "entity_id": "123",
 *   "action": "update",
 *   "data": {
 *     "nombreDeHorario": "Horario Escolar",
 *     "diasDeSemana": [1, 2, 3, 4, 5],
 *     "horaInicio": "08:00",
 *     "horaFin": "14:00",
 *     "isActive": true
 *   },
 *   "previous_data": {
 *     "horaInicio": "09:00",
 *     "horaFin": "15:00"
 *   },
 *   "created_at": "2025-06-29T10:30:00Z",
 *   "synced_at": "2025-06-29T10:31:00Z"
 * }
 */
data class SyncEvent(
    val id: Long,                              // ID único del evento
    val deviceId: String,                      // ID del dispositivo que generó el evento
    val entity_type: String,                   // "horario", "app", "device"
    val entity_id: String,                     // ID o packageName de la entidad
    val action: String,                        // "create", "update", "delete"
    val data: Map<String, Any>? = null,        // Datos actuales de la entidad
    val previous_data: Map<String, Any>? = null, // Datos anteriores (para rollback)
    val created_at: String,                    // Cuándo se creó el evento
    val synced_at: String? = null             // Cuándo fue sincronizado (null = pendiente)
) 