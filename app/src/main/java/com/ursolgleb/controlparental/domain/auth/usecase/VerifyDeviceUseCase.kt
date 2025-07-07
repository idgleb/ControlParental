package com.ursolgleb.controlparental.domain.auth.usecase

import com.ursolgleb.controlparental.domain.auth.model.DeviceToken
import com.ursolgleb.controlparental.domain.auth.model.VerificationCode
import com.ursolgleb.controlparental.domain.auth.repository.DeviceAuthRepository
import javax.inject.Inject

/**
 * Caso de uso para verificar un dispositivo con código
 */
class VerifyDeviceUseCase @Inject constructor(
    private val repository: DeviceAuthRepository
) {
    /**
     * Verificar dispositivo
     * @param verificationCode Código ingresado por el usuario
     * @param childName Nombre del niño (opcional)
     */
    suspend operator fun invoke(
        verificationCode: String,
        childName: String? = null
    ): Result<DeviceToken> {
        // Obtener device ID guardado
        val deviceId = repository.getDeviceId()
            ?: return Result.failure(IllegalStateException("Device not registered"))
        
        // Limpiar código de verificación
        val sanitizedCode = VerificationCode.sanitize(verificationCode)
        
        // Validar formato del código
        if (!sanitizedCode.matches(Regex("\\d{6}"))) {
            return Result.failure(IllegalArgumentException("Invalid verification code format"))
        }
        
        // Verificar con el servidor
        return repository.verifyDevice(
            deviceId = deviceId,
            verificationCode = sanitizedCode,
            childName = childName?.trim()
        ).onSuccess { token ->
            // Guardar token localmente
            repository.saveToken(token)
        }
    }
} 