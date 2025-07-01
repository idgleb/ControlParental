package com.ursolgleb.controlparental.data.remote.models

/**
 * Representa un evento individual que se sincroniza
 * Usado en endpoint: POST /api/sync/events (dentro del array "events" de PostEventsRequest)
 * Ejemplo JSON para CREATE/UPDATE:
 * {
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
 *   "timestamp": "2025-06-29T10:30:00Z"
 * }
 * 
 * Ejemplo JSON para DELETE:
 * {
 *   "entity_type": "app",
 *   "entity_id": "com.example.game",
 *   "action": "delete",
 *   "timestamp": "2025-06-29T10:31:00Z"
 * }
 */
data class EventDto(
    val entity_type: String,      // "horario", "app", "device"
    val entity_id: String,        // ID o packageName de la entidad
    val action: String,           // "create", "update", "delete"
    val data: Map<String, Any?>? = null,  // Datos de la entidad (solo para create/update)
    val timestamp: String         // ISO 8601 timestamp
) 