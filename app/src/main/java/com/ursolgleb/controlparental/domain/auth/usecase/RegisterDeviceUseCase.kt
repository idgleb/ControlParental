package com.ursolgleb.controlparental.domain.auth.usecase

import android.os.Build
import com.ursolgleb.controlparental.BuildConfig
import com.ursolgleb.controlparental.domain.auth.model.DeviceRegistration
import com.ursolgleb.controlparental.domain.auth.model.VerificationCode
import com.ursolgleb.controlparental.domain.auth.repository.DeviceAuthRepository
import com.ursolgleb.controlparental.domain.auth.repository.DeviceRegistrationResult
import javax.inject.Inject

/**
 * Caso de uso para registrar un dispositivo
 */
class RegisterDeviceUseCase @Inject constructor(
    private val repository: DeviceAuthRepository
) {
    /**
     * Ejecutar el registro del dispositivo
     */
    suspend operator fun invoke(): Result<DeviceRegistrationResult> {
        android.util.Log.d("RegisterDeviceUseCase", "invoke: Iniciando registro")
        
        // Obtener device ID existente o generar uno nuevo
        val deviceId = repository.getDeviceId() ?: java.util.UUID.randomUUID().toString()
        
        android.util.Log.d("RegisterDeviceUseCase", "invoke: deviceId = $deviceId")
        
        // Crear datos de registro con información del dispositivo
        val registration = DeviceRegistration(
            deviceId = deviceId,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            appVersion = BuildConfig.VERSION_NAME,
            manufacturer = Build.MANUFACTURER,
            fingerprint = Build.FINGERPRINT
        )
        
        android.util.Log.d("RegisterDeviceUseCase", "invoke: Llamando repository.registerDevice")
        // El repository guardará el ID solo si el registro es exitoso
        return repository.registerDevice(registration)
    }
} 