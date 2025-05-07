package com.ursolgleb.controlparental.data.apps.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "usage_limits",
    foreignKeys = [ForeignKey(
        entity = AppEntity::class,
        parentColumns = ["packageName"],
        childColumns = ["packageName"]
    )]
)
data class UsageLimitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val maxUsageMinutesPorDia: Int // Límite en minutos por día
)

