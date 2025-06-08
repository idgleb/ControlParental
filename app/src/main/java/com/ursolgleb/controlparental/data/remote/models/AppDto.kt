package com.ursolgleb.controlparental.data.remote.models

/**
 * DTO sencillo utilizado para sincronizar aplicaciones con el servidor.
 */
data class AppDto(
    val packageName: String,
    val appName: String,
    val appStatus: String,
    val dailyUsageLimitMinutes: Int,
    val usageTimeToday: Long
)
