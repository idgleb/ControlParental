package com.ursolgleb.controlparental.data.local.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.TypeConverters
import androidx.room.Index
import com.ursolgleb.controlparental.utils.Converters
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "horarios",
    primaryKeys = ["deviceId", "idHorario"], // Clave primaria compuesta
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["deviceId"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["deviceId", "idHorario"], unique = true)]
)
@TypeConverters(Converters::class)
data class HorarioEntity(
    val deviceId: String,
    val idHorario: Long, // ID de negocio que se usa junto con deviceId
    var nombreDeHorario: String,
    var diasDeSemana: List<Int>, // 1 = Lunes, ..., 7 = Domingo
    var horaInicio: String,   // Ej: LocalTime.of(8, 0) para 08:00
    var horaFin: String,      // Ej: LocalTime.of(20, 0) para 20:00
    var isActive: Boolean = true // Por defecto el horario está activo
) : Parcelable