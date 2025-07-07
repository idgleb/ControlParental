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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import javax.inject.Inject

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
    private val authRepository: DeviceAuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DeviceAuthUiState())
    val uiState: StateFlow<DeviceAuthUiState> = _uiState.asStateFlow()
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.NotRegistered)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    private var verificationCheckJob: Job? = null
    
    init {
        // Checkear estado inicial
        android.util.Log.d("DeviceAuthViewModel", "init: Iniciando checkAuthState")
        checkAuthState()
    }
    
    private fun checkAuthState() {
        viewModelScope.launch {
            try {
                android.util.Log.d("DeviceAuthViewModel", "checkAuthState: Verificando estado de autenticación")
                
                // Verificar si el dispositivo ya tiene token guardado
                val savedToken = authRepository.getSavedToken()
                val deviceId = authRepository.getDeviceId()
                
                android.util.Log.d("DeviceAuthViewModel", "checkAuthState: savedToken=$savedToken, deviceId=$deviceId")
                
                // Si tiene token y deviceId, SIEMPRE verificar con el servidor
                if (savedToken != null && deviceId != null) {
                    android.util.Log.d("DeviceAuthViewModel", "checkAuthState: Verificando con servidor...")
                    
                    // Mostrar estado de carga mientras verifica
                    _uiState.value = _uiState.value.copy(isLoading = true)
                    
                    checkDeviceStatusUseCase(deviceId).fold(
                        onSuccess = { token ->
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            
                            if (token != null) {
                                // Dispositivo existe y está verificado
                                _authState.value = AuthState.Authenticated(token)
                                android.util.Log.d("DeviceAuthViewModel", "checkAuthState: Dispositivo confirmado - Autenticado")
                            } else {
                                // Dispositivo existe pero no está verificado
                                android.util.Log.w("DeviceAuthViewModel", "checkAuthState: Dispositivo no verificado - limpiando")
                                authRepository.clearToken()
                                _authState.value = AuthState.NotRegistered
                                _uiState.value = _uiState.value.copy(
                                    registrationStep = RegistrationStep.INITIAL,
                                    error = "El dispositivo no está verificado. Registrándose nuevamente..."
                                )
                                delay(2000)
                                registerDevice()
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("DeviceAuthViewModel", "checkAuthState: Error verificando con servidor", error)
                            _uiState.value = _uiState.value.copy(isLoading = false)
                            
                            // Si el dispositivo no existe (404), limpiar y registrar de nuevo
                            if (error.message?.contains("404") == true || 
                                error.message?.contains("not found", ignoreCase = true) == true) {
                                android.util.Log.w("DeviceAuthViewModel", "checkAuthState: Dispositivo no existe - limpiando")
                                authRepository.clearToken()
                                _authState.value = AuthState.NotRegistered
                                _uiState.value = _uiState.value.copy(
                                    error = "El dispositivo fue eliminado del servidor. Registrándose nuevamente...",
                                    registrationStep = RegistrationStep.INITIAL
                                )
                                delay(2000)
                                _uiState.value = _uiState.value.copy(error = null)
                                registerDevice()
                            } else {
                                // Otro error - mostrar botón para reintentar
                                _authState.value = AuthState.Authenticated(savedToken)
                                _uiState.value = _uiState.value.copy(
                                    registrationStep = RegistrationStep.INITIAL,
                                    error = "No se pudo verificar con el servidor. Presiona el botón para reintentar."
                                )
                            }
                        }
                    )
                } else if (deviceId != null) {
                    // Solo tiene deviceId, no token - estado WaitingVerification
                    android.util.Log.d("DeviceAuthViewModel", "checkAuthState: Solo deviceId, esperando verificación")
                    _authState.value = AuthState.WaitingVerification
                    _uiState.value = _uiState.value.copy(
                        registrationStep = RegistrationStep.VERIFICATION_CODE
                    )
                    
                    // Si hay un código de verificación previo, iniciamos la verificación periódica
                    if (_uiState.value.verificationCode == null) {
                        android.util.Log.d("DeviceAuthViewModel", "checkAuthState: Iniciando verificación periódica desde estado WaitingVerification")
                        startPeriodicVerificationCheck()
                    }
                } else {
                    // No tiene nada - registrar nuevo
                    _authState.value = AuthState.NotRegistered
                    android.util.Log.d("DeviceAuthViewModel", "checkAuthState: No registrado - iniciando registro")
                    registerDevice()
                }
            } catch (e: Exception) {
                android.util.Log.e("DeviceAuthViewModel", "checkAuthState: Error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error al verificar estado: ${e.message}"
                )
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
                        
                        // Si el resultado indica que ya está verificado
                        if (result is VerificationCode && result.code == "ALREADY_VERIFIED") {
                            android.util.Log.d("DeviceAuthViewModel", "registerDevice: Dispositivo ya verificado, verificando estado...")
                            
                            // Cancelar cualquier verificación periódica en progreso
                            verificationCheckJob?.cancel()
                            
                            // Hacer check-status inmediatamente
                            _uiState.value = _uiState.value.copy(
                                isLoading = true,
                                error = null
                            )
                            
                            // Verificar estado inmediatamente
                            viewModelScope.launch {
                                val deviceId = authRepository.getDeviceId()
                                if (deviceId != null) {
                                    checkDeviceStatusUseCase(deviceId).fold(
                                        onSuccess = { token ->
                                            if (token != null) {
                                                android.util.Log.d("DeviceAuthViewModel", "Token obtenido exitosamente")
                                                _authState.value = AuthState.Authenticated(token)
                                                _uiState.value = _uiState.value.copy(
                                                    isLoading = false,
                                                    registrationStep = RegistrationStep.COMPLETED
                                                )
                                            } else {
                                                _uiState.value = _uiState.value.copy(
                                                    isLoading = false,
                                                    error = "No se pudo obtener el token de autenticación"
                                                )
                                            }
                                        },
                                        onFailure = { error ->
                                            _uiState.value = _uiState.value.copy(
                                                isLoading = false,
                                                error = "Error al verificar estado: ${error.message}"
                                            )
                                        }
                                    )
                                }
                            }
                        } else {
                            // Flujo normal de registro
                            android.util.Log.d("DeviceAuthViewModel", "registerDevice: Éxito - código: ${result.formatted()}")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                registrationStep = RegistrationStep.VERIFICATION_CODE,
                                verificationCode = result
                            )
                            // Iniciar verificación periódica
                            startPeriodicVerificationCheck()
                        }
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
                        
                        // Cancelar verificación periódica si hay error
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
                            } else {
                                android.util.Log.d("DeviceAuthViewModel", "check-status devolvió null - dispositivo no verificado aún")
                            }
                        },
                        onFailure = { error ->
                            android.util.Log.e("DeviceAuthViewModel", "Error verificando estado con servidor", error)
                            
                            // Si el dispositivo fue eliminado, detener la verificación y reiniciar
                            if (error.message?.contains("404") == true || 
                                error.message?.contains("not found", ignoreCase = true) == true) {
                                android.util.Log.w("DeviceAuthViewModel", "Dispositivo eliminado durante verificación")
                                isVerified = true // Salir del loop
                                
                                // Limpiar y reiniciar
                                authRepository.clearToken()
                                _authState.value = AuthState.NotRegistered
                                _uiState.value = _uiState.value.copy(
                                    error = "El dispositivo fue eliminado. Reiniciando registro...",
                                    registrationStep = RegistrationStep.INITIAL
                                )
                                
                                delay(2000)
                                checkAuthState()
                            }
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
        viewModelScope.launch {
            android.util.Log.d("DeviceAuthViewModel", "forceNewRegistration: Forzando nuevo registro")
            
            // Cancelar cualquier verificación en progreso
            verificationCheckJob?.cancel()
            
            // Limpiar todas las credenciales locales
            authRepository.clearToken()
            
            // Resetear estados
            _authState.value = AuthState.NotRegistered
            _uiState.value = DeviceAuthUiState(
                isLoading = false,
                error = null,
                registrationStep = RegistrationStep.INITIAL,
                verificationCode = null
            )
            
            // Pequeño delay para asegurar que todo se limpió
            delay(100)
            
            // Registrar dispositivo directamente
            registerDevice()
        }
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
} 