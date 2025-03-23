package com.ursolgleb.controlparental.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "horarios")
data class HorarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombreDeHorario: String,
    val diasDeSemana: List<Int>, // 1 = Lunes, ..., 7 = Domingo
    val horaInicio: String,      // Ej: "08:00"
    val horaFin: String          // Ej: "20:00"
)
