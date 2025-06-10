package com.ursolgleb.controlparental.data.remote.models

import android.graphics.Bitmap

/**
 * DTO sencillo utilizado para sincronizar aplicaciones con el servidor.
 */
data class AppDto(
    val packageName: String?,
    val deviceId: String?,
    val appName: String?,
    val appIcon: ByteArray?,
    var appCategory: String,
    var contentRating: String,
    var isSystemApp: Boolean,
    var usageTimeToday: Long?,
    var timeStempUsageTimeToday: Long,
    val appStatus: String?,
    val dailyUsageLimitMinutes: Int?,
)
