package com.ursolgleb.controlparental.data.apps.entities

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    primaryKeys = ["packageName", "horarioId"],
    tableName = "app_horario_cross_ref",
    foreignKeys = [
        ForeignKey(
            entity = AppEntity::class,
            parentColumns = ["packageName"],
            childColumns = ["packageName"],
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
    val horarioId: Int
)