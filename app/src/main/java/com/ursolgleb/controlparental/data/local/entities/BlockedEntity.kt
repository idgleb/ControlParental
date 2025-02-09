package com.ursolgleb.controlparental.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "blocked",
    foreignKeys = [ForeignKey(
        entity = AppEntity::class,
        parentColumns = ["packageName"],
        childColumns = ["packageName"]
    )]
)
data class BlockedEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String
)

