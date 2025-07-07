package com.ursolgleb.controlparental.domain.auth.model

/**
 * Modelo de dominio para el código de verificación
 */
data class VerificationCode(
    val code: String,
    val expiresInMinutes: Int
) {
    init {
        require(code.matches(Regex("\\d{6}"))) { 
            "Verification code must be 6 digits" 
        }
        require(expiresInMinutes > 0) { 
            "Expiration time must be positive" 
        }
    }
    
    /**
     * Formatear el código para mostrar al usuario (XXX-XXX)
     */
    fun formatted(): String {
        return "${code.substring(0, 3)}-${code.substring(3, 6)}"
    }
    
    /**
     * Limpiar el código ingresado por el usuario
     */
    companion object {
        fun sanitize(input: String): String {
            return input.replace(Regex("[^0-9]"), "")
        }
    }
} 