package com.ursolgleb.controlparental.data.apps.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_stats")
data class UsageStatsEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val dia: Long,
    val usageDuration: Long
)
