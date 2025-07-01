package com.ursolgleb.controlparental.data.remote.models

/**
 * Evento enviado por el cliente (similar a EventDto)
 * Nota: Actualmente se usa EventDto en lugar de este modelo
 * Si se usara, sería en endpoint: POST /api/sync/events
 * Ejemplo JSON para CREATE/UPDATE:
 * {
 *   "entity_type": "horario",
 *   "entity_id": "123",
 *   "action": "update",
 *   "data": {
 *     "nombreDeHorario": "Horario Nocturno",
 *     "diasDeSemana": [5, 6, 7],
 *     "horaInicio": "20:00",
 *     "horaFin": "22:00",
 *     "isActive": true
 *   },
 *   "timestamp": "2025-06-29T10:30:00.000Z"
 * }
 * 
 * Ejemplo JSON para DELETE:
 * {
 *   "entity_type": "app",
 *   "entity_id": "com.social.media",
 *   "action": "delete",
 *   "timestamp": "2025-06-29T10:31:00.000Z"
 * }
 */
data class ClientSyncEvent(
    val entity_type: String,              // "horario", "app", "device"
    val entity_id: String,                // ID o packageName de la entidad
    val action: String,                   // "create", "update", "delete"
    val data: Map<String, Any?>? = null,  // Datos de la entidad (solo para create/update)
    val timestamp: String                 // ISO 8601 timestamp con millisegundos
) 