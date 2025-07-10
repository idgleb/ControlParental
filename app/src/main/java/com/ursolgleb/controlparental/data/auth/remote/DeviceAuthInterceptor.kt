package com.ursolgleb.controlparental.data.auth.remote

import com.ursolgleb.controlparental.data.auth.local.DeviceAuthLocalDataSource
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor para agregar autenticación a las peticiones HTTP
 */
@Singleton
class DeviceAuthInterceptor @Inject constructor(
    private val localDataSource: DeviceAuthLocalDataSource
) : Interceptor {
    
    companion object {
        // Rutas que no requieren autenticación
        private val PUBLIC_PATHS = listOf(
            "/v1/auth/register",
            "/v1/auth/verify",
            "/v1/auth/check-status",
            "/health"
        )
    }
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()
        
        // No agregar auth a rutas públicas
        if (isPublicPath(originalRequest)) {
            android.util.Log.d("DeviceAuthInterceptor", "Ruta pública, sin auth: $url")
            return chain.proceed(originalRequest)
        }
        
        // Obtener token guardado
        val token = localDataSource.getApiToken()
        
        val request = if (token != null) {
            android.util.Log.d("DeviceAuthInterceptor", "Agregando auth headers a: $url")
            android.util.Log.d("DeviceAuthInterceptor", "Token: ${token.token.take(10)}..., DeviceId: ${token.deviceId}")
            originalRequest.newBuilder()
                .header("Authorization", token.toAuthHeader())
                .header("X-Device-Token", token.token)
                .header("X-Device-ID", token.deviceId)
                .build()
        } else {
            android.util.Log.w("DeviceAuthInterceptor", "No hay token para: $url")
            originalRequest
        }
        
        return chain.proceed(request)
    }
    
    private fun isPublicPath(request: Request): Boolean {
        val path = request.url.encodedPath
        return PUBLIC_PATHS.any { publicPath ->
            path.endsWith(publicPath) || path.contains(publicPath)
        }
    }
} 