package com.ursolgleb.controlparental.data.log

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val blockedAt: Long = System.currentTimeMillis()
)
