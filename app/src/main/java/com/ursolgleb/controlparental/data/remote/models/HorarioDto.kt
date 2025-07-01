package com.ursolgleb.controlparental.data.remote.models

/**
 * DTO utilizado para sincronizar horarios con el servidor
 * Usado en endpoints:
 * - GET /api/sync/horarios?deviceId={deviceId} (dentro de SyncResponse<HorarioDto>)
 * - POST /api/sync/horarios (array de HorarioDto)
 * - DELETE /api/sync/horarios?deviceId={deviceIds}
 * - POST /api/sync/events (dentro del campo "data" de EventDto)
 * Ejemplo JSON:
 * {
 *   "idHorario": 123,
 *   "deviceId": "abc-123-def-456",
 *   "nombreDeHorario": "Horario Escolar",
 *   "diasDeSemana": [1, 2, 3, 4, 5],
 *   "horaInicio": "08:00",
 *   "horaFin": "14:00",
 *   "isActive": true
 * }
 * 
 * diasDeSemana: 1=Lunes, 2=Martes, 3=Miércoles, 4=Jueves, 5=Viernes, 6=Sábado, 7=Domingo
 */
data class HorarioDto(
    val idHorario: Long,              // ID único del horario
    val deviceId: String?,            // ID del dispositivo asociado
    val nombreDeHorario: String,      // Nombre descriptivo del horario
    val diasDeSemana: List<Int>,      // Lista de días (1-7)
    val horaInicio: String?,          // Hora de inicio en formato "HH:mm"
    val horaFin: String?,             // Hora de fin en formato "HH:mm"
    val isActive: Boolean             // Si el horario está activo
)
