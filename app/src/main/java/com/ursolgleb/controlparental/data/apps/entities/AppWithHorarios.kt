package com.ursolgleb.controlparental.data.apps.entities

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

/**
 * Representa un AppEntity junto con los horarios asociados.
 */
data class AppWithHorarios(
    @Embedded val app: AppEntity,
    @Relation(
        parentColumn = "packageName",
        entityColumn = "id",
        associateBy = Junction(
            value = AppHorarioCrossRef::class,
            parentColumn = "packageName",
            entityColumn = "horarioId"
        )
    )
    val horarios: List<HorarioEntity>
)