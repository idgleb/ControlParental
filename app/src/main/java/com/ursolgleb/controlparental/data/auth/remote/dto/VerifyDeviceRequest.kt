package com.ursolgleb.controlparental.data.auth.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request para verificar dispositivo
 */
@JsonClass(generateAdapter = true)
data class VerifyDeviceRequest(
    @Json(name = "device_id")
    val deviceId: String,
    
    @Json(name = "verification_code")
    val verificationCode: String,
    
    @Json(name = "child_name")
    val childName: String? = null
)

/**
 * Response de verificación de dispositivo
 */
@JsonClass(generateAdapter = true)
data class VerifyDeviceResponse(
    @Json(name = "success")
    val success: Boolean,
    
    @Json(name = "data")
    val data: VerifyDeviceData? = null,
    
    @Json(name = "error")
    val error: String? = null,
    
    @Json(name = "code")
    val errorCode: String? = null
)

@JsonClass(generateAdapter = true)
data class VerifyDeviceData(
    @Json(name = "device_id")
    val deviceId: String,
    
    @Json(name = "api_token")
    val apiToken: String,
    
    @Json(name = "message")
    val message: String
) 