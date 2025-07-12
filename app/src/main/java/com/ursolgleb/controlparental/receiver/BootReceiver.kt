package com.ursolgleb.controlparental.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import android.os.Build
import com.ursolgleb.controlparental.ControlParentalApp
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.services.AppBlockerService
import com.ursolgleb.controlparental.workers.ModernSyncWorker
import dagger.hilt.android.AndroidEntryPoint
import com.ursolgleb.controlparental.data.auth.local.DeviceAuthLocalDataSource
import com.ursolgleb.controlparental.services.HeartbeatService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.ursolgleb.controlparental.services.ServiceStarter

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    
    @Inject
    lateinit var deviceAuthLocalDataSource: DeviceAuthLocalDataSource
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent != null && (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED)) {
            Log.d("BootReceiver", "Dispositivo reiniciado o app actualizada")

            Log.d("BootReceiver", "onReceive: ${intent?.action}")
            // Usar el sistema moderno de sincronización basado en eventos
            ModernSyncWorker.startWorker(context)
            
            // Iniciar HeartbeatService si hay credenciales
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val hasToken = deviceAuthLocalDataSource.getApiToken() != null
                    if (hasToken) {
                        Log.d("BootReceiver", "Token encontrado, iniciando HeartbeatService")
                        ServiceStarter.startBackgroundServices(context)
                    } else {
                        Log.d("BootReceiver", "No hay token, HeartbeatService no se iniciará")
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error verificando token para HeartbeatService BootReceiver", e)
                }
            }

            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val expectedService = "${context.packageName}/.services.AppBlockerService"

            if (enabledServices?.contains(expectedService) == true) {
                Log.d("BootReceiver", "✅ AppBlockerService está activado.")
            } else {
                Log.w("BootReceiver", "⚠️ AppBlockerService NO está activado.")
                sendAccessibilityReminderNotification(context)
            }
        }
    }

    private fun sendAccessibilityReminderNotification(context: Context) {
        val channelId = "accessibility_reminder"
        val notificationId = 1001

        val settingsIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            settingsIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Crear canal de notificación (solo Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Activar AppBlockerService",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificación para recordar activar el servicio de accesibilidad"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.exclamo_round)
            .setContentTitle("Activar AppBlockerService")
            .setContentText("Abre los ajustes y activa el servicio de accesibilidad.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
    }
}

