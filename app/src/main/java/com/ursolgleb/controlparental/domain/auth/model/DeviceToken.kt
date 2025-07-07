package com.ursolgleb.controlparental.domain.auth.model

/**
 * Modelo de dominio para el token de autenticación del dispositivo
 */
data class DeviceToken(
    val token: String,
    val deviceId: String
) {
    init {
        require(token.isNotBlank()) { "Token cannot be blank" }
        require(token.length >= 40) { "Token too short" }
        require(deviceId.isNotBlank()) { "Device ID cannot be blank" }
    }
    
    /**
     * Obtener el token formateado para headers HTTP
     */
    fun toAuthHeader(): String = "Bearer $token"
} 