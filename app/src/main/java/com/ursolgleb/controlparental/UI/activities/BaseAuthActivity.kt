package com.ursolgleb.controlparental.UI.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ursolgleb.controlparental.data.auth.local.DeviceAuthLocalDataSource
import com.ursolgleb.controlparental.presentation.auth.DeviceAuthActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Actividad base que verifica si hay credenciales de autenticación al inicio
 * Todas las actividades principales deben extender de esta clase
 */
@AndroidEntryPoint
abstract class BaseAuthActivity : AppCompatActivity() {
    
    @Inject
    lateinit var authLocalDataSource: DeviceAuthLocalDataSource
    
    companion object {
        private const val TAG = "BaseAuthActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Solo verificar credenciales locales al inicio
        if (!isAuthenticationActivity()) {
            checkInitialAuth()
        }
    }
    
    /**
     * Verifica si hay credenciales locales al iniciar la actividad
     * NO hace llamadas al servidor para evitar regeneración de tokens
     */
    private fun checkInitialAuth() {
        // Acceso directo y síncrono a SharedPreferences
        val deviceId = authLocalDataSource.getDeviceId()
        val token = authLocalDataSource.getApiToken()
        
        if (deviceId == null || token == null) {
            android.util.Log.w(TAG, "No hay credenciales locales - redirigiendo a autenticación")
            navigateToAuth()
        } else {
            android.util.Log.d(TAG, "Credenciales locales encontradas - deviceId: $deviceId")
        }
    }
    
    /**
     * Navega a la pantalla de autenticación
     */
    protected fun navigateToAuth() {
        android.util.Log.d(TAG, "Navegando a DeviceAuthActivity")
        val intent = Intent(this, DeviceAuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("redirected_from", this@BaseAuthActivity::class.java.simpleName)
        }
        startActivity(intent)
        finish()
    }
    
    /**
     * Verifica si la actividad actual es una actividad de autenticación
     */
    private fun isAuthenticationActivity(): Boolean {
        val activityClass = this::class.java
        return activityClass == DeviceAuthActivity::class.java || 
               activityClass == AuthActivity::class.java
    }
    
    /**
     * Método helper para verificar si hay autenticación válida
     */
    protected fun hasValidAuth(): Boolean {
        return authLocalDataSource.getDeviceId() != null && 
               authLocalDataSource.getApiToken() != null
    }
    
    /**
     * Método helper para obtener el deviceId actual
     */
    protected fun getCurrentDeviceId(): String? {
        return authLocalDataSource.getDeviceId()
    }
} 