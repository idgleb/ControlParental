package com.ursolgleb.controlparental.data.auth.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request para registrar un dispositivo
 */
@JsonClass(generateAdapter = true)
data class RegisterDeviceRequest(
    @Json(name = "device_id")
    val deviceId: String,
    
    @Json(name = "model")
    val model: String,
    
    @Json(name = "android_version")
    val androidVersion: String,
    
    @Json(name = "app_version")
    val appVersion: String,
    
    @Json(name = "manufacturer")
    val manufacturer: String? = null,
    
    @Json(name = "fingerprint")
    val fingerprint: String? = null
) 