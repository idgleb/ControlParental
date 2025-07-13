package com.ursolgleb.controlparental.services

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.os.Build
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Bundle
import com.ursolgleb.controlparental.handlers.SyncHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import com.ursolgleb.controlparental.data.local.AppDataRepository
import com.ursolgleb.controlparental.data.remote.RemoteDataRepository
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Job

@AndroidEntryPoint
class LocationWatcherService : Service() {
    companion object {
        private const val TAG = "LocationWatcherService"
        private const val CHANNEL_ID = "location_watcher_channel"
        private const val NOTIFICATION_ID = 2

        fun start(context: Context) {
            val intent = Intent(context, LocationWatcherService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LocationWatcherService::class.java)
            context.stopService(intent)
        }
    }

    @Inject
    lateinit var syncHandler: SyncHandler

    @Inject
    lateinit var localRepo: AppDataRepository

    @Inject
    lateinit var remoteRepo: RemoteDataRepository

    private var locationManager: LocationManager? = null
    private var lastLocation: Location? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var locationJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LocationWatcherService created")
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "LocationWatcherService started")
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        startLocationUpdates()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "LocationWatcherService destroyed")
        stopLocationUpdates()
        locationJob?.cancel()
        serviceScope.cancel()
    }

    private fun startLocationUpdates() {
        locationJob?.cancel()
        locationJob = serviceScope.launch {
            if (ActivityCompat.checkSelfPermission(this@LocationWatcherService, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this@LocationWatcherService, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "No location permission, not starting updates")
                return@launch
            }
            try {
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10_000L, // 10 segundos
                    5f, // 5 metros
                    locationListener,
                    Looper.getMainLooper()
                )
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    10_000L,
                    5f,
                    locationListener,
                    Looper.getMainLooper()
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error starting location updates", e)
            }
        }
    }

    private fun stopLocationUpdates() {
        try {
            locationManager?.removeUpdates(locationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping location updates", e)
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            Log.d(TAG, "Location changed: lat=${location.latitude}, lon=${location.longitude}")
            val changed = lastLocation == null ||
                location.latitude != lastLocation?.latitude ||
                location.longitude != lastLocation?.longitude
            lastLocation = location
            if (changed) {
                serviceScope.launch {
                    try {
                        val device = localRepo.getDeviceInfoOnce()
                        if (device != null) {
                            val response = remoteRepo.sendHeartbeat(
                                deviceId = device.deviceId,
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                            if (response.isSuccessful) {
                                Log.d(TAG, "Ubicación enviada al backend correctamente")
                            } else {
                                Log.e(TAG, "Error enviando ubicación al backend: ${response.code()}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Excepción enviando ubicación al backend", e)
                    }
                }
            }
        }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ubicación en segundo plano",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Observando ubicación")
            .setContentText("La app está observando cambios de ubicación para sincronización.")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }
} 