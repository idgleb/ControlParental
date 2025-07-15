package com.ursolgleb.controlparental.presentation.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ursolgleb.controlparental.BuildConfig
import com.ursolgleb.controlparental.UI.activities.AuthActivity
import com.ursolgleb.controlparental.databinding.ActivityDeviceAuthBinding
import com.ursolgleb.controlparental.domain.auth.repository.AuthState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Activity para autenticación del dispositivo
 */
@AndroidEntryPoint
class DeviceAuthActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityDeviceAuthBinding
    private val viewModel: DeviceAuthViewModel by viewModels()
    
    // Para evitar múltiples clics
    private var lastClickTime = 0L
    private val CLICK_THROTTLE_DELAY = 1000L // 1 segundo entre clics
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("DeviceAuthActivity", "onCreate: Iniciando")
        
        // Verificar si venimos de un interceptor
        val errorMessage = intent.getStringExtra("error_message")
        if (errorMessage != null) {
            Log.w("DeviceAuthActivity", "onCreate: Redirigido desde interceptor - $errorMessage")
            Toast.makeText(this, "Sesión cerrada: $errorMessage", Toast.LENGTH_LONG).show()
        }
        
        // Configurar DataBinding
        binding = ActivityDeviceAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        Log.d("DeviceAuthActivity", "onCreate: ViewModel creado")
        
        setupViews()
        observeViewModel()
    }
    
    private fun setupViews() {
        with(binding) {
            // Botón de registro
            btnRegisterDevice.setOnClickListener {
                if (isClickAllowed()) {
                    if (viewModel.puedeIntentarRegistro()) {
                        viewModel.registerDevice(
                            onSuccess = { finish() },
                            onFailure = { error ->
                                Log.e("DeviceAuthActivity", "Registro fallido: $error")
                            }
                        )
                    } else {
                        binding.tvError.text = "No se puede registrar ahora. Espera unos minutos o pulsa 'Reintentar' más tarde."
                        binding.tvError.isVisible = true
                    }
                }
            }
            
            // Botón para solicitar nuevo código
            btnRequestNewCode.setOnClickListener {
                if (isClickAllowed()) {
                    viewModel.registerDevice() // Solicitar nuevo código
                }
            }
            // Botón de reintento de recuperación de token
            btnRetry.setOnClickListener {
                if (isClickAllowed()) {
                    if (viewModel.puedeIntentarRegistro()) {
                        viewModel.reintentarRegistro()
                    } else {
                        binding.tvError.text = "No se puede volver a intentar hasta que el servidor esté disponible o haya pasado el tiempo de espera."
                        binding.tvError.isVisible = true
                    }
                }
            }
            // Cerrar error al tocar
            tvError.setOnClickListener {
                viewModel.clearError()
            }
            
            // DEBUG: Botón temporal para limpiar credenciales
            if (BuildConfig.DEBUG) {
                tvWelcomeTitle.setOnLongClickListener {
                    viewModel.clearAllCredentials()
                    tvError.text = "Credenciales limpiadas"
                    tvError.isVisible = true
                    true
                }
            }
        }
    }
    
    private fun isClickAllowed(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime < CLICK_THROTTLE_DELAY) {
            return false
        }
        lastClickTime = currentTime
        return true
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observar estado de autenticación
                launch {
                    viewModel.authState.collect { authState ->
                        Log.d("DeviceAuthActivity", "authState cambió a: $authState")
                        when (authState) {
                            is AuthState.Authenticated -> {
                                Log.d("DeviceAuthActivity", "Estado Authenticated detectado - navegando a main")
                                // Ir a la actividad principal
                                navigateToMain()
                            }
                            is AuthState.WaitingVerification -> {
                                Log.d("DeviceAuthActivity", "Estado WaitingVerification detectado")
                                // Mostrar vista de verificación si no está ya visible
                                if (viewModel.uiState.value.registrationStep == RegistrationStep.INITIAL) {
                                    showVerificationView()
                                }
                            }
                            else -> {
                                Log.d("DeviceAuthActivity", "Otro estado: $authState")
                                // Mantener en pantalla de auth
                            }
                        }
                    }
                }
                
                // Observar estado de UI
                launch {
                    viewModel.uiState.collect { state ->
                        updateUI(state)
                    }
                }
            }
        }
    }
    
    private fun updateUI(state: DeviceAuthUiState) {
        with(binding) {
            // Loading
            progressBar.isVisible = state.isLoading
            
            // Error
            state.error?.let {
                showError(it)
                tvError.text = it
                tvError.isVisible = true
            } ?: run {
                tvError.isVisible = false
            }

            // Mostrar botón de reintento solo si se puede reintentar
            btnRetry.isVisible = state.error != null
            btnRetry.isEnabled = viewModel.puedeIntentarRegistro()
            btnRegisterDevice.isEnabled = viewModel.puedeIntentarRegistro()

            // Vistas según el paso
            when (state.registrationStep) {
                RegistrationStep.INITIAL -> {
                    groupInitial.isVisible = true
                    groupVerificationCode.isVisible = false
                    
                    // Si está cargando y no hay error, mostrar mensaje de registro automático
                    if (state.isLoading && state.error == null) {
                        tvWelcomeSubtitle.text = "Registrando dispositivo automáticamente..."
                        btnRegisterDevice.isVisible = false
                    } else {
                        // Verificar si hay credenciales guardadas
                        val hasStoredCredentials = viewModel.authState.value is AuthState.Authenticated
                        
                        if (hasStoredCredentials) {
                            tvWelcomeSubtitle.text = "Este dispositivo parece estar registrado. Si fue eliminado del servidor, presiona el botón para registrarlo nuevamente."
                            btnRegisterDevice.text = "Registrar Nuevamente"
                        } else {
                            tvWelcomeSubtitle.text = "Para comenzar, necesitas registrar este dispositivo con tu cuenta de padre"
                            btnRegisterDevice.text = "Registrar Dispositivo"
                        }
                        btnRegisterDevice.isVisible = true
                    }
                }
                RegistrationStep.VERIFICATION_CODE -> {
                    groupInitial.isVisible = false
                    groupVerificationCode.isVisible = true
                    
                    // Mostrar código generado
                    state.verificationCode?.let { code ->
                        tvVerificationCode.text = code.formatted()
                    }
                }
                RegistrationStep.COMPLETED -> {
                    // La navegación se maneja en authState
                }
            }
            
            // Habilitar/deshabilitar botones
            btnRegisterDevice.isEnabled = !state.isLoading
            btnRequestNewCode.isEnabled = !state.isLoading
        }
    }
    
    private fun showVerificationView() {
        binding.groupInitial.isVisible = false
        binding.groupVerificationCode.isVisible = true
    }
    
    private fun showError(message: String) {
        binding.tvError.apply {
            text = message
            isVisible = true
        }
    }
    
    private fun navigateToMain() {
        Log.d("DeviceAuthActivity", "navigateToMain: Navegando a AuthActivity")
        
        // Los servicios de background ya se iniciaron en el ViewModel cuando la autenticación fue exitosa
        
        // Navegar a la actividad principal existente
        val intent = Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
        Log.d("DeviceAuthActivity", "navigateToMain: Navegación completada")
    }
} 