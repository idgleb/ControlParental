package com.ursolgleb.controlparental.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked",
    foreignKeys = [ForeignKey(
        entity = AppEntity::class,
        parentColumns = ["packageName"],
        childColumns = ["packageName"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BlockedEntity(
    @PrimaryKey val packageName: String,
    val timestamp: Long = System.currentTimeMillis() // Guarda el momento de inserción
)

