package com.ursolgleb.controlparental.data.log

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_blocked_apps")
data class LogBlockedAppEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String,
    val blockedAt: Long = System.currentTimeMillis()
)
