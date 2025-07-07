package com.ursolgleb.controlparental.domain.auth.model

import java.util.UUID

/**
 * Modelo de dominio para el registro de dispositivos
 */
data class DeviceRegistration(
    val deviceId: String = UUID.randomUUID().toString(),
    val model: String,
    val androidVersion: String,
    val appVersion: String,
    val manufacturer: String? = null,
    val fingerprint: String? = null
) {
    init {
        require(model.isNotBlank()) { "Model cannot be blank" }
        require(androidVersion.isNotBlank()) { "Android version cannot be blank" }
        require(appVersion.isNotBlank()) { "App version cannot be blank" }
        require(isValidUUID(deviceId)) { "Invalid device ID format" }
    }
    
    private fun isValidUUID(uuid: String): Boolean {
        return try {
            UUID.fromString(uuid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
} 