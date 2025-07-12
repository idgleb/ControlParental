package com.ursolgleb.controlparental.domain.auth.repository

import com.ursolgleb.controlparental.domain.auth.model.DeviceRegistration
import com.ursolgleb.controlparental.domain.auth.model.DeviceToken
import com.ursolgleb.controlparental.domain.auth.model.VerificationCode
import kotlinx.coroutines.flow.Flow

/**
 * Resultado del registro de dispositivo
 */
sealed class DeviceRegistrationResult {
    data class NewCode(val verificationCode: VerificationCode) : DeviceRegistrationResult()
    object AlreadyVerified : DeviceRegistrationResult()
    data class AlreadyVerifiedButFailed(val error: Throwable) : DeviceRegistrationResult()
}

/**
 * Repositorio para manejo de autenticación de dispositivos
 */
interface DeviceAuthRepository {
    
    /**
     * Registrar un nuevo dispositivo
     * @return Resultado del registro (código de verificación o ya verificado)
     */
    suspend fun registerDevice(registration: DeviceRegistration): Result<DeviceRegistrationResult>
    
    /**
     * Verificar dispositivo con código
     * @param deviceId ID del dispositivo
     * @param verificationCode Código de verificación
     * @param childName Nombre opcional del niño
     * @return Token de autenticación
     */
    suspend fun verifyDevice(
        deviceId: String,
        verificationCode: String,
        childName: String? = null
    ): Result<DeviceToken>
    
    /**
     * Obtener token guardado localmente
     */
    suspend fun getSavedToken(): DeviceToken?
    
    /**
     * Guardar token localmente
     */
    suspend fun saveToken(token: DeviceToken)
    
    /**
     * Limpiar token guardado
     */
    suspend fun clearToken()
    
    /**
     * Verificar si el dispositivo está registrado
     */
    suspend fun isDeviceRegistered(): Boolean
    
    /**
     * Obtener ID del dispositivo guardado
     */
    suspend fun getDeviceId(): String?
    
    /**
     * Guardar ID del dispositivo
     */
    suspend fun saveDeviceId(deviceId: String)
    
    /**
     * Stream del estado de autenticación
     */
    fun observeAuthState(): Flow<AuthState>
}

/**
 * Estados de autenticación del dispositivo
 */
sealed class AuthState {
    object NotRegistered : AuthState()
    object WaitingVerification : AuthState()
    data class Authenticated(val token: DeviceToken) : AuthState()
    object Unauthenticated : AuthState()
} 