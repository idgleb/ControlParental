package com.ursolgleb.controlparental.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val appIcon: String,
    val appCategory: String,
    val contentRating: String,
    val appIsSystemApp: Boolean,
    val tiempoUsoSeconds: Long
)
