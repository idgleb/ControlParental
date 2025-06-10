package com.ursolgleb.controlparental.data.apps.entities

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    primaryKeys = ["packageName", "deviceId", "horarioId"],
    tableName = "app_horario_cross_ref",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["packageName", "deviceId"],
            childColumns = ["packageName", "deviceId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = HorarioEntity::class,
            parentColumns = ["id"],
            childColumns = ["horarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AppHorarioCrossRef(
    val packageName: String,
    val deviceId: String,
    val horarioId: Long
)