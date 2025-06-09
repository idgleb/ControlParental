package com.ursolgleb.controlparental.data.remote.models

/**
 * DTO utilizado para sincronizar horarios con el servidor.
 */
data class HorarioDto(
    val id: Long,
    val nombreDeHorario: String,
    val diasDeSemana: List<Int>,
    val horaInicio: String?,
    val horaFin: String?,
    val isActive: Boolean
)
