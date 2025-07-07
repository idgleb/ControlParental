package com.ursolgleb.controlparental.data.auth.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.ursolgleb.controlparental.data.auth.remote.dto.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Servicio API para autenticación de dispositivos
 */
interface DeviceAuthApiService {
    
    /**
     * Registrar un nuevo dispositivo
     */
    @POST("v1/auth/register")
    suspend fun registerDevice(
        @Body request: RegisterDeviceRequest
    ): RegisterDeviceResponse
    
    /**
     * Verificar dispositivo con código
     */
    @POST("v1/auth/verify")
    suspend fun verifyDevice(
        @Body request: VerifyDeviceRequest
    ): VerifyDeviceResponse
    
    /**
     * Obtener estado del dispositivo
     */
    @GET("v1/auth/status")
    suspend fun getDeviceStatus(
        @Header("Authorization") token: String
    ): DeviceStatusResponse
    
    /**
     * Verificar estado del dispositivo (sin autenticación)
     * Usado para verificar si fue verificado desde la web
     */
    @GET("v1/auth/check-status")
    suspend fun checkDeviceStatus(
        @Query("device_id") deviceId: String
    ): CheckDeviceStatusResponse
}

/**
 * Response del estado del dispositivo
 */
@JsonClass(generateAdapter = true)
data class DeviceStatusResponse(
    @Json(name = "success")
    val success: Boolean,
    
    @Json(name = "data")
    val data: DeviceStatusData? = null
)

@JsonClass(generateAdapter = true)
data class DeviceStatusData(
    @Json(name = "device_id")
    val deviceId: String,
    
    @Json(name = "is_verified")
    val isVerified: Boolean,
    
    @Json(name = "is_active")
    val isActive: Boolean,
    
    @Json(name = "is_blocked")
    val isBlocked: Boolean
)

/**
 * Response de verificación de estado sin autenticación
 */
@JsonClass(generateAdapter = true)
data class CheckDeviceStatusResponse(
    @Json(name = "success")
    val success: Boolean,
    
    @Json(name = "data")
    val data: CheckDeviceStatusData? = null,
    
    @Json(name = "error")
    val error: String? = null
)

@JsonClass(generateAdapter = true)
data class CheckDeviceStatusData(
    @Json(name = "device_id")
    val deviceId: String,
    
    @Json(name = "is_verified")
    val isVerified: Boolean,
    
    @Json(name = "api_token")
    val apiToken: String? = null,
    
    @Json(name = "child_name")
    val childName: String? = null
) 