package com.ursolgleb.controlparental.domain.auth.usecase

import com.ursolgleb.controlparental.data.auth.remote.DeviceAuthApiService
import com.ursolgleb.controlparental.domain.auth.exception.RateLimitException
import com.ursolgleb.controlparental.domain.auth.model.DeviceToken
import com.ursolgleb.controlparental.domain.auth.repository.DeviceAuthRepository
import retrofit2.HttpException
import javax.inject.Inject

/**
 * Caso de uso para verificar el estado del dispositivo en el servidor
 */
class CheckDeviceStatusUseCase @Inject constructor(
    private val repository: DeviceAuthRepository,
    private val apiService: DeviceAuthApiService
) {
    /**
     * Verificar si el dispositivo fue verificado desde la web
     */
    suspend operator fun invoke(deviceId: String): Result<DeviceToken?> {
        return try {
            android.util.Log.d("CheckDeviceStatusUseCase", "Verificando estado para deviceId: $deviceId")
            
            // Hacer petición al servidor para verificar estado
            val response = apiService.checkDeviceStatus(deviceId)
            
            android.util.Log.d("CheckDeviceStatusUseCase", "Respuesta del servidor: success=${response.success}, " +
                    "isVerified=${response.data?.isVerified}, hasToken=${response.data?.apiToken != null}, " +
                    "error=${response.error}")
            
            // Verificar si la respuesta indica que el dispositivo no fue encontrado
            if (!response.success && response.error?.contains("not found", ignoreCase = true) == true) {
                android.util.Log.w("CheckDeviceStatusUseCase", "Dispositivo no encontrado: ${response.error}")
                return Result.failure(Exception("Device not found (404)"))
            }
            
            if (response.success && response.data?.isVerified == true && response.data.apiToken != null) {
                android.util.Log.d("CheckDeviceStatusUseCase", "Dispositivo verificado! Token recibido: ${response.data.apiToken.take(10)}...")
                
                // Crear y guardar token
                val token = DeviceToken(
                    token = response.data.apiToken,
                    deviceId = deviceId
                )
                
                // Guardar token localmente
                android.util.Log.d("CheckDeviceStatusUseCase", "Guardando token para deviceId: $deviceId")
                repository.saveToken(token)
                android.util.Log.d("CheckDeviceStatusUseCase", "Token guardado exitosamente")
                
                Result.success(token)
            } else {
                android.util.Log.d("CheckDeviceStatusUseCase", "Dispositivo aún no verificado o sin token")
                Result.success(null)
            }
        } catch (e: Exception) {
            android.util.Log.e("CheckDeviceStatusUseCase", "Error verificando estado", e)
            
            // Manejar específicamente errores de rate limiting
            if (e is HttpException) {
                when (e.code()) {
                    429 -> {
                        val retryAfter = e.response()?.headers()?.get("Retry-After")?.toIntOrNull() ?: 60
                        Result.failure(RateLimitException(retryAfter))
                    }
                    404 -> {
                        // Dispositivo no encontrado - propagar con mensaje claro
                        android.util.Log.w("CheckDeviceStatusUseCase", "Dispositivo no encontrado en servidor")
                        Result.failure(Exception("Device not found (404)"))
                    }
                    else -> Result.failure(e)
                }
            } else {
                Result.failure(e)
            }
        }
    }
} 