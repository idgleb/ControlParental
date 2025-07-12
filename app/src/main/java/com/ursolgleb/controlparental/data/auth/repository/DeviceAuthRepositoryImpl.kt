package com.ursolgleb.controlparental.data.auth.repository

import com.ursolgleb.controlparental.data.auth.local.DeviceAuthLocalDataSource
import com.ursolgleb.controlparental.data.auth.remote.DeviceAuthApiService
import com.ursolgleb.controlparental.data.auth.remote.dto.RegisterDeviceRequest
import com.ursolgleb.controlparental.data.auth.remote.dto.VerifyDeviceRequest
import com.ursolgleb.controlparental.domain.auth.exception.RateLimitException
import com.ursolgleb.controlparental.domain.auth.model.DeviceRegistration
import com.ursolgleb.controlparental.domain.auth.model.DeviceToken
import com.ursolgleb.controlparental.domain.auth.model.VerificationCode
import com.ursolgleb.controlparental.domain.auth.repository.AuthState
import com.ursolgleb.controlparental.domain.auth.repository.DeviceAuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import com.squareup.moshi.JsonDataException

/**
 * Implementación del repositorio de autenticación
 */
@Singleton
class DeviceAuthRepositoryImpl @Inject constructor(
    private val apiService: DeviceAuthApiService,
    private val localDataSource: DeviceAuthLocalDataSource
) : DeviceAuthRepository {
    
    // 1. Agregar sealed class para el resultado del registro
    sealed class DeviceRegistrationResult {
        data class NewCode(val verificationCode: VerificationCode) : DeviceRegistrationResult()
        object AlreadyVerified : DeviceRegistrationResult()
        data class AlreadyVerifiedButFailed(val error: Throwable) : DeviceRegistrationResult()
    }
    
    override suspend fun registerDevice(
        registration: DeviceRegistration
    ): Result<DeviceRegistrationResult> {
        return try {
            android.util.Log.d("DeviceAuthRepositoryImpl", "registerDevice: Iniciando con deviceId=${registration.deviceId}")
            
            val request = RegisterDeviceRequest(
                deviceId = registration.deviceId,
                model = registration.model,
                androidVersion = registration.androidVersion,
                appVersion = registration.appVersion,
                manufacturer = registration.manufacturer,
                fingerprint = registration.fingerprint
            )
            
            android.util.Log.d("DeviceAuthRepositoryImpl", "registerDevice: Llamando API")
            val response = apiService.registerDevice(request)
            android.util.Log.d("DeviceAuthRepositoryImpl", "registerDevice: Respuesta recibida - success=${response.success}")
            
            if (response.success && response.data != null) {
                if (response.data.isAlreadyVerified) {
                    // Flujo automático: intentar recuperar el token
                    val tokenResult = fetchTokenIfAlreadyVerified(response.data.deviceId)
                    return if (tokenResult.isSuccess) {
                        // Token recuperado y guardado
                        android.util.Log.d("DeviceAuthRepositoryImpl", "registerDevice: Token recuperado automáticamente tras AlreadyVerified")
                        Result.success(DeviceRegistrationResult.AlreadyVerified)
                    } else {
                        android.util.Log.e("DeviceAuthRepositoryImpl", "registerDevice: No se pudo recuperar el token tras AlreadyVerified")
                        Result.success(DeviceRegistrationResult.AlreadyVerifiedButFailed(tokenResult.exceptionOrNull() ?: Exception("Error desconocido")))
                    }
                } else if (response.data.verificationCode != null && response.data.expiresInMinutes != null) {
                    // Guardar device ID localmente SOLO si el registro fue exitoso
                    localDataSource.saveDeviceId(registration.deviceId)
                    
                    val code = VerificationCode(
                        code = response.data.verificationCode.replace("-", ""),
                        expiresInMinutes = response.data.expiresInMinutes
                    )
                    
                    android.util.Log.d("DeviceAuthRepositoryImpl", "registerDevice: Éxito - código=${code.formatted()}")
                    Result.success(DeviceRegistrationResult.NewCode(code))
                } else {
                    android.util.Log.e("DeviceAuthRepositoryImpl", "registerDevice: Respuesta inesperada, faltan campos")
                    Result.failure(Exception("Respuesta inesperada del servidor: faltan campos de verificación"))
                }
            } else {
                android.util.Log.e("DeviceAuthRepositoryImpl", "registerDevice: Error en respuesta - ${response.error}")
                Result.failure(
                    Exception(response.error ?: "Registration failed")
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("DeviceAuthRepositoryImpl", "registerDevice: Excepción", e)
            
            // Manejar específicamente errores de rate limiting
            if (e is HttpException && e.code() == 429) {
                // Intentar obtener el header Retry-After
                val retryAfter = e.response()?.headers()?.get("Retry-After")?.toIntOrNull() ?: 60
                android.util.Log.d("DeviceAuthRepositoryImpl", "Rate limit exceeded. Retry after: $retryAfter seconds")
                Result.failure(RateLimitException(retryAfter))
            } else if (e is SocketTimeoutException || e is UnknownHostException || e is JsonDataException) {
                android.util.Log.e("DeviceAuthRepositoryImpl", "registerDevice: Error de red o parsing, no se borra el token", e)
                Result.failure(e)
            } else {
                android.util.Log.e("DeviceAuthRepositoryImpl", "registerDevice: Error no autenticación, posible borrado de token", e)
                // Otros errores, posiblemente de autenticación
                Result.failure(e)
            }
        }
    }
    
    // 3. Método privado para recuperar el token automáticamente
    private suspend fun fetchTokenIfAlreadyVerified(deviceId: String): Result<DeviceToken> {
        return try {
            val response = apiService.checkDeviceStatus(deviceId)
            if (response.success && response.data != null && response.data.apiToken != null) {
                val token = DeviceToken(
                    token = response.data.apiToken,
                    deviceId = deviceId
                )
                localDataSource.saveApiToken(token)
                Result.success(token)
            } else {
                Result.failure(Exception("No se pudo recuperar el token"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun verifyDevice(
        deviceId: String,
        verificationCode: String,
        childName: String?
    ): Result<DeviceToken> {
        return try {
            val request = VerifyDeviceRequest(
                deviceId = deviceId,
                verificationCode = verificationCode,
                childName = childName
            )
            
            val response = apiService.verifyDevice(request)
            
            if (response.success && response.data != null) {
                val token = DeviceToken(
                    token = response.data.apiToken,
                    deviceId = deviceId
                )
                
                Result.success(token)
            } else {
                Result.failure(
                    Exception(response.error ?: "Verification failed")
                )
            }
        } catch (e: Exception) {
            // Manejar específicamente errores de rate limiting
            if (e is HttpException && e.code() == 429) {
                val retryAfter = e.response()?.headers()?.get("Retry-After")?.toIntOrNull() ?: 60
                Result.failure(RateLimitException(retryAfter))
            } else if (e is SocketTimeoutException || e is UnknownHostException || e is JsonDataException) {
                android.util.Log.e("DeviceAuthRepositoryImpl", "verifyDevice: Error de red o parsing, no se borra el token", e)
                Result.failure(e)
            } else {
                android.util.Log.e("DeviceAuthRepositoryImpl", "verifyDevice: Error no autenticación, posible borrado de token", e)
                // Otros errores, posiblemente de autenticación
                Result.failure(e)
            }
        }
    }
    
    override suspend fun getSavedToken(): DeviceToken? {
        return localDataSource.getApiToken()
    }
    
    override suspend fun saveToken(token: DeviceToken) {
        android.util.Log.d("DeviceAuthRepositoryImpl", "saveToken: Guardando token para deviceId=${token.deviceId}, token=${token.token.take(10)}...")
        localDataSource.saveApiToken(token)
        android.util.Log.d("DeviceAuthRepositoryImpl", "saveToken: Token guardado exitosamente")
    }
    
    override suspend fun clearToken() {
        localDataSource.clearCredentials()
    }
    
    override suspend fun isDeviceRegistered(): Boolean {
        return localDataSource.isDeviceRegistered()
    }
    
    override suspend fun getDeviceId(): String? {
        return localDataSource.getDeviceId()
    }
    
    override suspend fun saveDeviceId(deviceId: String) {
        localDataSource.saveDeviceId(deviceId)
    }
    
    override fun observeAuthState(): Flow<AuthState> {
        return localDataSource.authStateFlow.map { isAuthenticated ->
            val token = localDataSource.getApiToken()
            when {
                !localDataSource.isDeviceRegistered() -> AuthState.NotRegistered
                localDataSource.isDeviceRegistered() && !localDataSource.isDeviceVerified() -> AuthState.WaitingVerification
                isAuthenticated && token != null -> AuthState.Authenticated(token)
                else -> AuthState.Unauthenticated
            }
        }
    }
} 