package com.ursolgleb.controlparental.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ursolgleb.controlparental.domain.auth.exception.RateLimitException
import com.ursolgleb.controlparental.domain.auth.model.DeviceToken
import com.ursolgleb.controlparental.domain.auth.model.VerificationCode
import com.ursolgleb.controlparental.domain.auth.repository.AuthState
import com.ursolgleb.controlparental.domain.auth.repository.DeviceAuthRepository
import com.ursolgleb.controlparental.domain.auth.usecase.RegisterDeviceUseCase
import com.ursolgleb.controlparental.domain.auth.usecase.VerifyDeviceUseCase
import com.ursolgleb.controlparental.domain.auth.usecase.CheckDeviceStatusUseCase
import com.ursolgleb.controlparental.utils.createRateLimitMessage
import com.ursolgleb.controlparental.workers.ModernSyncWorker
import com.ursolgleb.controlparental.services.HeartbeatService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import javax.inject.Inject
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ursolgleb.controlparental.domain.auth.repository.DeviceRegistrationResult

/**
 * Estados del proceso de registro
 */
enum class RegistrationStep {
    INITIAL,            // Pantalla inicial - botón registrar
    VERIFICATION_CODE,  // Mostrando código de verificación
    COMPLETED          // Proceso completado
}

/**
 * Estado de UI para la pantalla de autenticación
 */
data class DeviceAuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val registrationStep: RegistrationStep = RegistrationStep.INITIAL,
    val verificationCode: VerificationCode? = null
)

/**
 * ViewModel para la autenticación del dispositivo
 */
@HiltViewModel
class DeviceAuthViewModel @Inject constructor(
    private val registerDeviceUseCase: RegisterDeviceUseCase,
    private val verifyDeviceUseCase: VerifyDeviceUseCase,
    private val checkDeviceStatusUseCase: CheckDeviceStatusUseCase,
    private val authRepository: DeviceAuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DeviceAuthUiState())
    val uiState: StateFlow<DeviceAuthUiState> = _uiState.asStateFlow()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotRegistered)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private var verificationCheckJob: Job? = null
    
    init {
        android.util.Log.d("DeviceAuthViewModel", "init: Iniciando checkAuthState")
        checkAuthState()
    }
    
    /**
     * Verifica el estado de autenticación actual
     */
    private fun checkAuthState() {
        android.util.Log.d("DeviceAuthViewModel", "checkAuthState: Verificando estado de autenticación")
        viewModelScope.launch {
            val savedToken = authRepository.getSavedToken()
            val deviceId = authRepository.getDeviceId()
            
            android.util.Log.d("DeviceAuthViewModel", "checkAuthState: savedToken=$savedToken, deviceId=$deviceId")
            
            when {
                savedToken == null || deviceId == null -> {
                    // No hay credenciales guardadas
                    android.util.Log.d("DeviceAuthViewModel", "checkAuthState: No registrado - iniciando registro")
                    _authState.value = AuthState.NotRegistered
                    registerDevice()
                }
                else -> {
                    // Hay credenciales, verificar con el servidor
                    android.util.Log.d("DeviceAuthViewModel", "checkAuthState: Verificando con servidor")
                    checkDeviceStatusUseCase(deviceId).fold(
                        onSuccess = { token ->
                            if (token != null) {
                                android.util.Log.d("DeviceAuthViewModel", "checkAuthState: Dispositivo verificado con token existente")
                                _authState.value = AuthState.Authenticated(token)
                                _uiState.value = _uiState.value.copy(
                                    registrationStep = RegistrationStep.COMPLETED
                                )
                                // Reiniciar el worker de sincronización
                                startBackgroundServices()
                            } else {
                                android.util.Log.d("DeviceAuthViewModel", "checkAuthState: Dispositivo no verificado aún")
                                _authState.value = AuthState.NotRegistered
                                registerDevice()
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("DeviceAuthViewModel", "checkAuthState: Error verificando con servidor", error)
                            if (error.message?.contains("404") == true || 
                                error.message?.contains("not found", ignoreCase = true) == true) {
                                // Dispositivo eliminado
                                android.util.Log.w("DeviceAuthViewModel", "checkAuthState: Dispositivo eliminado")
                                authRepository.clearToken()
                                _authState.value = AuthState.NotRegistered
                                registerDevice()
                            } else {
                                // Otro error, asumir no registrado
                                _authState.value = AuthState.NotRegistered
                                registerDevice()
                            }
                        }
                    )
                }
            }
        }
    }
    
    /**
     * Registrar el dispositivo
     */
    fun registerDevice() {
        android.util.Log.d("DeviceAuthViewModel", "registerDevice: Iniciando registro")
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                android.util.Log.d("DeviceAuthViewModel", "registerDevice: Llamando registerDeviceUseCase")
                
                registerDeviceUseCase().fold(
                    onSuccess = { result ->
                        android.util.Log.d("DeviceAuthViewModel", "registerDevice: Resultado recibido")
                        handleRegistrationResult(result)
                    },
                    onFailure = { error ->
                        android.util.Log.e("DeviceAuthViewModel", "registerDevice: Error", error)
                        val errorMessage = when (error) {
                            is RateLimitException -> createRateLimitMessage(error.retryAfterSeconds)
                            else -> error.message ?: "Error al registrar dispositivo"
                        }
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = errorMessage
                        )
                        verificationCheckJob?.cancel()
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("DeviceAuthViewModel", "registerDevice: Excepción no manejada", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error inesperado al registrar dispositivo"
                )
            }
        }
    }
    
    private fun handleRegistrationResult(result: DeviceRegistrationResult) {
        when (result) {
            is DeviceRegistrationResult.AlreadyVerified -> {
                verificationCheckJob?.cancel()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    registrationStep = RegistrationStep.COMPLETED,
                    error = null
                )
            }
            is DeviceRegistrationResult.AlreadyVerifiedButFailed -> {
                verificationCheckJob?.cancel()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    registrationStep = RegistrationStep.INITIAL,
                    error = "El dispositivo ya estaba verificado, pero no se pudo recuperar el token automáticamente. ${result.error.message ?: ""}"
                )
            }
            is DeviceRegistrationResult.NewCode -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    registrationStep = RegistrationStep.VERIFICATION_CODE,
                    verificationCode = result.verificationCode
                )
                startPeriodicVerificationCheck()
            }
        }
    }
    
    /**
     * Iniciar verificación periódica del estado del dispositivo
     */
    private fun startPeriodicVerificationCheck() {
        // Cancelar job anterior si existe
        verificationCheckJob?.cancel()
        
        verificationCheckJob = viewModelScope.launch {
            val deviceId = authRepository.getDeviceId() ?: return@launch
            var isVerified = false
            
            while (!isVerified) {
                delay(3000) // Verificar cada 3 segundos
                
                try {
                    android.util.Log.d("DeviceAuthViewModel", "Verificando estado del dispositivo...")
                    
                    // Primero verificar si ya tenemos token local
                    val localToken = authRepository.getSavedToken()
                    if (localToken != null) {
                        android.util.Log.d("DeviceAuthViewModel", "Token local encontrado")
                        _authState.value = AuthState.Authenticated(localToken)
                        isVerified = true
                        continue
                    }
                    
                    // Si no hay token local, verificar con el servidor
                    checkDeviceStatusUseCase(deviceId).fold(
                        onSuccess = { token ->
                            if (token != null) {
                                android.util.Log.d("DeviceAuthViewModel", "Dispositivo verificado desde la web! Token recibido: ${token.token.take(10)}...")
                                
                                // Cambiar estado de autenticación
                                _authState.value = AuthState.Authenticated(token)
                                
                                // Verificar que se guardó correctamente
                                val savedToken = authRepository.getSavedToken()
                                android.util.Log.d("DeviceAuthViewModel", "Token guardado en repository: ${savedToken?.token?.take(10)}...")
                                
                                isVerified = true
                                
                                // Actualizar UI para mostrar que se completó
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    registrationStep = RegistrationStep.COMPLETED,
                                    error = null
                                )
                                
                                // Reiniciar el worker de sincronización
                                startBackgroundServices()
                            } else {
                                android.util.Log.d("DeviceAuthViewModel", "check-status devolvió null - dispositivo no verificado aún")
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("DeviceAuthViewModel", "Error verificando estado", error)
                        }
                    )
                    
                } catch (e: Exception) {
                    android.util.Log.e("DeviceAuthViewModel", "Error verificando estado", e)
                }
            }
        }
    }
    
    /**
     * Verificar el dispositivo con el código
     */
    fun verifyDevice(code: String, childName: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            verifyDeviceUseCase(code, childName).fold(
                onSuccess = { token ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        registrationStep = RegistrationStep.COMPLETED
                    )
                    // Cambiar estado de autenticación
                    _authState.value = AuthState.Authenticated(token)
                    
                    // Reiniciar el worker de sincronización
                    startBackgroundServices()
                },
                onFailure = { error ->
                    val errorMessage = when (error) {
                        is RateLimitException -> createRateLimitMessage(error.retryAfterSeconds)
                        else -> error.message ?: "Error al verificar código"
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                }
            )
        }
    }
    
    /**
     * Forzar un nuevo registro limpiando cualquier credencial existente
     */
    fun forceNewRegistration() {
        registerDevice()
    }
    
    /**
     * Limpiar error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Limpiar todas las credenciales (para debug)
     */
    fun clearAllCredentials() {
        viewModelScope.launch {
            android.util.Log.d("DeviceAuthViewModel", "clearAllCredentials: Limpiando todas las credenciales")
            
            // Cancelar cualquier verificación en progreso
            verificationCheckJob?.cancel()
            
            // Limpiar credenciales
            authRepository.clearToken()
            
            // Resetear estados
            _authState.value = AuthState.NotRegistered
            _uiState.value = DeviceAuthUiState(
                isLoading = false,
                error = null,
                registrationStep = RegistrationStep.INITIAL,
                verificationCode = null
            )
            
            // Pequeño delay para que la UI se actualice
            delay(100)
            
            // Reiniciar el proceso
            checkAuthState()
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        verificationCheckJob?.cancel()
    }
    
    /**
     * Reinicia el worker de sincronización para usar el nuevo deviceId
     */
    private fun restartSyncWorker() {
        try {
            android.util.Log.d("DeviceAuthViewModel", "Reiniciando ModernSyncWorker con nuevo deviceId")
            ModernSyncWorker.startWorker(context)
        } catch (e: Exception) {
            android.util.Log.e("DeviceAuthViewModel", "Error reiniciando ModernSyncWorker", e)
        }
    }
    
    /**
     * Inicia el servicio de heartbeat después de autenticación exitosa
     */
    fun startHeartbeatService() {
        try {
            android.util.Log.d("DeviceAuthViewModel", "Iniciando HeartbeatService")
            HeartbeatService.start(context)
        } catch (e: Exception) {
            android.util.Log.e("DeviceAuthViewModel", "Error iniciando HeartbeatService", e)
        }
    }
    
    /**
     * Inicia todos los servicios necesarios después de autenticación exitosa
     */
    private fun startBackgroundServices() {
        android.util.Log.d("DeviceAuthViewModel", "Iniciando servicios de background después de autenticación exitosa")
        
        // Iniciar el worker de sincronización
        restartSyncWorker()
        
        // Iniciar el servicio de heartbeat
        try {
            android.util.Log.d("DeviceAuthViewModel", "Iniciando HeartbeatService")
            HeartbeatService.start(context)
        } catch (e: Exception) {
            android.util.Log.e("DeviceAuthViewModel", "Error iniciando HeartbeatService", e)
        }
    }
} 