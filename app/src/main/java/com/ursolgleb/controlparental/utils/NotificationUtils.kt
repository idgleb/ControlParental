package com.ursolgleb.controlparental.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.ursolgleb.controlparental.R

object NotificationUtils {
    private const val CHANNEL_ID = "heartbeat_channel"
    private const val CHANNEL_NAME = "Heartbeat Service"

    fun createHeartbeatNotification(context: Context): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Control Parental activo")
            .setContentText("Monitoreando ubicación y estado del dispositivo")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }
} 