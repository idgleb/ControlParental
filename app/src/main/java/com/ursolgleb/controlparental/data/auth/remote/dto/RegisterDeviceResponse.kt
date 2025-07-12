package com.ursolgleb.controlparental.data.auth.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response del registro de dispositivo
 */
@JsonClass(generateAdapter = true)
data class RegisterDeviceResponse(
    @Json(name = "success")
    val success: Boolean,
    
    @Json(name = "data")
    val data: RegisterDeviceData? = null,
    
    @Json(name = "error")
    val error: String? = null,
    
    @Json(name = "code")
    val errorCode: String? = null
)

@JsonClass(generateAdapter = true)
data class RegisterDeviceData(
    @Json(name = "device_id")
    val deviceId: String,
    
    @Json(name = "verification_code")
    val verificationCode: String?,
    
    @Json(name = "expires_in_minutes")
    val expiresInMinutes: Int?,
    
    @Json(name = "is_already_verified")
    val isAlreadyVerified: Boolean = false,
    
    @Json(name = "message")
    val message: String? = null
) 