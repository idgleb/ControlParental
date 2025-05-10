package com.ursolgleb.controlparental.data.apps.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ursolgleb.controlparental.utils.Converters
import java.time.LocalTime

@Entity(tableName = "horarios")
@TypeConverters(Converters::class)
data class HorarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombreDeHorario: String,
    val diasDeSemana: List<Int>, // 1 = Lunes, ..., 7 = Domingo
    val horaInicio: LocalTime,   // Ej: LocalTime.of(8, 0) para 08:00
    val horaFin: LocalTime,      // Ej: LocalTime.of(20, 0) para 20:00
    var isActive: Boolean = true // Por defecto el horario está activo
)