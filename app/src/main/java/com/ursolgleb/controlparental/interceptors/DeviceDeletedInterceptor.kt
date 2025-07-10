package com.ursolgleb.controlparental.interceptors

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ursolgleb.controlparental.data.auth.local.DeviceAuthLocalDataSource
import com.ursolgleb.controlparental.presentation.auth.DeviceAuthActivity
import com.ursolgleb.controlparental.services.HeartbeatService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceDeletedInterceptor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceAuthLocalDataSource: DeviceAuthLocalDataSource
) : Interceptor {
    
    companion object {
        private const val TAG = "DeviceDeletedInterceptor"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        
        // Verificar códigos de error de autenticación
        when (response.code) {
            401 -> {
                // No autorizado - el dispositivo fue eliminado o no existe
                handleDeviceDeleted(response.code, response.message)
            }
            403 -> {
                // Prohibido - podría ser token expirado o dispositivo bloqueado
                // Por ahora tratarlo como dispositivo eliminado
                handleDeviceDeleted(response.code, response.message)
            }
            404 -> {
                // No encontrado - el dispositivo no existe en el servidor
                handleDeviceDeleted(response.code, response.message)
            }
        }
        
        return response
    }
    
    private fun handleDeviceDeleted(code: Int, message: String) {
        Log.d(TAG, "handleDeviceDeleted: $code - $message")
        
        CoroutineScope(Dispatchers.IO).launch {
            // Detener el servicio de heartbeat antes de limpiar las credenciales
            Log.d(TAG, "Deteniendo HeartbeatService...")
            HeartbeatService.stop(context)
            
            // Para errores 401/403/404, limpiar el registro pero mantener el deviceId
            // Esto permite que la app siga funcionando offline
            deviceAuthLocalDataSource.clearRegistration()
            Log.d(TAG, "handleDeviceDeleted: Registro eliminado, datos locales mantenidos")
        }
        
        // Redirigir a la pantalla de autenticación
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent(context, DeviceAuthActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("error_message", "$code - $message")
            }
            context.startActivity(intent)
        }
    }
} 