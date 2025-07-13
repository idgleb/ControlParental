package com.ursolgleb.controlparental.services

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import com.ursolgleb.controlparental.data.local.AppDataRepository
import com.ursolgleb.controlparental.data.remote.RemoteDataRepository
import com.ursolgleb.controlparental.handlers.SyncHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import android.os.BatteryManager
import android.os.Build
import com.ursolgleb.controlparental.data.local.entities.DeviceEntity
import android.location.LocationListener
import android.os.Bundle
import android.os.Looper
import androidx.annotation.RequiresPermission
import androidx.core.location.LocationManagerCompat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import android.content.pm.ServiceInfo

@AndroidEntryPoint
class HeartbeatService : Service() {
    
    companion object {
        private const val TAG = "HeartbeatService"
        private const val DEFAULT_INTERVAL_SECONDS = 4
        
        fun start(context: Context) {
            val intent = Intent(context, HeartbeatService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, HeartbeatService::class.java)
            context.stopService(intent)
        }
    }
    
    @Inject
    lateinit var localRepo: AppDataRepository
    
    @Inject
    lateinit var remoteRepo: RemoteDataRepository
    
    @Inject
    lateinit var syncHandler: SyncHandler
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private var locationManager: LocationManager? = null
    
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "HeartbeatService created")
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "HeartbeatService started")
        val notification = com.ursolgleb.controlparental.utils.NotificationUtils.createHeartbeatNotification(this)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(1, notification)
        }
        startHeartbeat()
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null

    @RequiresPermission(Manifest.permission.SCHEDULE_EXACT_ALARM)
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "HeartbeatService destroyed")
        heartbeatJob?.cancel()
        serviceScope.cancel()
        // Eliminar lógica de reinicio con AlarmManager. El reinicio periódico debe hacerse con WorkManager.
    }
    
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                try {
                    sendHeartbeat()
                    delay(DEFAULT_INTERVAL_SECONDS * 1000L)
                } catch (e: IllegalStateException) {
                    // Si no hay credenciales, detener el servicio
                    Log.e(TAG, "No credentials available, stopping service: ${e.message}")
                    //stopSelf()
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error in heartbeat loop", e)
                    // Si es un error genérico, intentar continuar después de un delay
                    delay(DEFAULT_INTERVAL_SECONDS * 1000L)
                }
            }
        }
    }
    
    private suspend fun sendHeartbeat() {
        try {
            val device = localRepo.getDeviceInfoOnce() ?: run {
                Log.e(TAG, "No device info available")
                return
            }
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager
            val currentBattery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val currentModel = "${Build.MANUFACTURER} ${Build.MODEL}"

            // Verificar si hay cambios en el dispositivo
            var hasDeviceChanges = false
            if (device.model != currentModel || device.batteryLevel != currentBattery) {
                hasDeviceChanges = true
            }

            // Enviar heartbeat al servidor (sin ubicación)
            val response = remoteRepo.sendHeartbeat(
                deviceId = device.deviceId,
                latitude = null,
                longitude = null
            )

            if (response.isSuccessful) {
                Log.d(TAG, "Heartbeat sent successfully (sin ubicación)")
                val updatedDevice = device.copy(
                    model = currentModel,
                    batteryLevel = currentBattery,
                    lastSeen = System.currentTimeMillis(),
                    pingIntervalSeconds = DEFAULT_INTERVAL_SECONDS
                )
                localRepo.updateDeviceInfo(updatedDevice)
                if (hasDeviceChanges) {
                    syncHandler.markDeviceUpdatePending()
                    Log.w(TAG, "Device info changed (battery/model), marked for sync")
                }
            } else {
                Log.e(TAG, "Heartbeat failed: ${response.code()}")
                if (response.code() == 401 || response.code() == 403) {
                    throw IllegalStateException("Authentication error: ${response.code()}")
                }
            }
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error sending heartbeat", e)
        }
    }
} 