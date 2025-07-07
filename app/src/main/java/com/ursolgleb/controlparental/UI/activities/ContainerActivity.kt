package com.ursolgleb.controlparental.UI.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.domain.auth.repository.DeviceAuthRepository
import com.ursolgleb.controlparental.domain.auth.usecase.CheckDeviceStatusUseCase
import com.ursolgleb.controlparental.presentation.auth.DeviceAuthActivity
import com.ursolgleb.controlparental.utils.NavBarUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ContainerActivity : AppCompatActivity() {

    @Inject
    lateinit var workManager: WorkManager
    
    @Inject
    lateinit var authRepository: DeviceAuthRepository
    
    @Inject
    lateinit var checkDeviceStatusUseCase: CheckDeviceStatusUseCase

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Mostrar una pantalla de carga simple mientras verifica
        setContentView(android.widget.ProgressBar(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.view.Gravity.CENTER
            )
        })
        
        // Verificar autenticación antes de mostrar contenido
        checkAuthentication()
    }
    
    private fun checkAuthentication() {
        lifecycleScope.launch {
            val token = authRepository.getSavedToken()
            val deviceId = authRepository.getDeviceId()
            
            if (token == null || deviceId == null) {
                // No hay token o deviceId guardado, ir a registro/autenticación
                navigateToAuth()
            } else {
                // Verificar con el servidor si el dispositivo aún existe
                try {
                    checkDeviceStatusUseCase(deviceId).fold(
                        onSuccess = { serverToken ->
                            if (serverToken != null) {
                                // Dispositivo verificado, mostrar contenido normal
                                setContentView(R.layout.activity_container)
                                NavBarUtils.aplicarEstiloNavBar(this@ContainerActivity)
                            } else {
                                // Dispositivo no verificado en servidor
                                authRepository.clearToken()
                                navigateToAuth()
                            }
                        },
                        onFailure = { error ->
                            // Si el dispositivo no existe (404), limpiar y redirigir
                            if (error.message?.contains("404") == true || 
                                error.message?.contains("not found", ignoreCase = true) == true) {
                                authRepository.clearToken()
                                navigateToAuth()
                            } else {
                                // Otro error, continuar con el token local por ahora
                                setContentView(R.layout.activity_container)
                                NavBarUtils.aplicarEstiloNavBar(this@ContainerActivity)
                            }
                        }
                    )
                } catch (e: Exception) {
                    // En caso de error de red, continuar con token local
                    setContentView(R.layout.activity_container)
                    NavBarUtils.aplicarEstiloNavBar(this@ContainerActivity)
                }
            }
        }
    }
    
    private fun navigateToAuth() {
        val intent = Intent(this@ContainerActivity, DeviceAuthActivity::class.java)
        startActivity(intent)
        finish()
    }
}