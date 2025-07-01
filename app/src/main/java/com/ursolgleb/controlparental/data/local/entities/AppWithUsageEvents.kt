package com.ursolgleb.controlparental.data.local.entities

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Representa un AppEntity junto con los UsageEventEntity asociados.
 */
data class AppWithUsageEvents(
    @Embedded val app: AppEntity,
    @Relation(
        parentColumn = "packageName",
        entityColumn = "packageName"
    )
    val events: List<UsageEventEntity>
)