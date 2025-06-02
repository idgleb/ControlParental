package com.ursolgleb.controlparental.data.apps.entities

import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
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
