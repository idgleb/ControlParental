package com.ursolgleb.controlparental.interceptors

import android.content.Context
import android.content.Intent
import android.util.Log
import com.ursolgleb.controlparental.data.auth.local.DeviceAuthLocalDataSource
import com.ursolgleb.controlparental.presentation.auth.DeviceAuthActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton
import android.app.ActivityOptions
import android.app.PendingIntent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.WindowManager


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

            redirectToAuth(context, "$code - $message")

        }
    }

    private fun redirectToAuth(context: Context, errorMessage: String) {
        val intent = Intent(context, DeviceAuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("error_message", errorMessage)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // ✅ Android 14+ - usar ActivityOptions para permitir el launch en background
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val options = ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                )
            }
            try {
                pendingIntent.send(
                    context, 0, null,
                    null, null, null,
                    options.toBundle()
                )
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }

        } else {
            // ⚠️ Android ≤ 13 – requiere overlay para poder traer app al frente
            if (Settings.canDrawOverlays(context)) {
                val windowManager =
                    context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                val dummyView = View(context)
                val params = WindowManager.LayoutParams(
                    1, 1,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                )

                // Agrega una view invisible para forzar foreground
                windowManager.addView(dummyView, params)

                // Espera un poco y luego lanza la Activity
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        windowManager.removeView(dummyView)
                    }
                }, 300)
            } else {
                // Si no tiene permiso de overlay, no puede forzar la apertura
                Log.w("RedirectToAuth", "Permiso SYSTEM_ALERT_WINDOW no concedido")
            }
        }
    }

} 