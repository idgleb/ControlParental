package com.ursolgleb.controlparental.data.local.entities

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "apps",
    primaryKeys = ["packageName", "deviceId"],
    foreignKeys = [
        ForeignKey(
            entity = DeviceEntity::class,
            parentColumns = ["deviceId"],
            childColumns = ["deviceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class AppEntity(
    val packageName: String,
    val deviceId: String,
    var appName: String,
    var appIcon: Bitmap,
    var appCategory: String,
    var contentRating: String,
    var isSystemApp: Boolean,
    var usageTimeToday: Long,
    var timeStempUsageTimeToday: Long,
    var appStatus: String,
    var dailyUsageLimitMinutes: Int,
    )
