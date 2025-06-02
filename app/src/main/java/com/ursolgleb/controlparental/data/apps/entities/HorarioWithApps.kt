package com.ursolgleb.controlparental.data.apps.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Representa un HorarioEntity junto con las apps asociadas.
 */
data class HorarioWithApps(
    @Embedded val horario: HorarioEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "packageName",
        associateBy = Junction(
            value = AppHorarioCrossRef::class,
            parentColumn = "horarioId",
            entityColumn = "packageName"
        )
    )
    val apps: List<AppEntity>
)