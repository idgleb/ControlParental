package com.ursolgleb.controlparental.data.apps.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ursolgleb.controlparental.utils.Converters
import kotlinx.parcelize.Parcelize
import java.time.LocalTime

@Parcelize
@Entity(tableName = "horarios")
@TypeConverters(Converters::class)
data class HorarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    var nombreDeHorario: String,
    var diasDeSemana: List<Int>, // 1 = Lunes, ..., 7 = Domingo
    var horaInicio: LocalTime,   // Ej: LocalTime.of(8, 0) para 08:00
    var horaFin: LocalTime,      // Ej: LocalTime.of(20, 0) para 20:00
    var isActive: Boolean = true // Por defecto el horario está activo
): Parcelable