package com.ursolgleb.controlparental.services

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
import androidx.core.location.LocationManagerCompat
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@AndroidEntryPoint
class HeartbeatService : Service() {
    
    companion object {
        private const val TAG = "HeartbeatService"
        private const val DEFAULT_INTERVAL_SECONDS = 30
        
        fun start(context: Context) {
            val intent = Intent(context, HeartbeatService::class.java)
            context.startService(intent)
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
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "HeartbeatService started")
        startHeartbeat()
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "HeartbeatService destroyed")
        heartbeatJob?.cancel()
        serviceScope.cancel()
    }
    
    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                try {
                    sendHeartbeat()
                    
                    // Obtener el intervalo configurado desde el dispositivo
                    val device = localRepo.getDeviceInfoOnce()
                    val intervalSeconds = device?.pingIntervalSeconds ?: DEFAULT_INTERVAL_SECONDS
                    
                    delay(intervalSeconds * 1000L)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in heartbeat loop", e)
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
            
            // Obtener información actualizada del dispositivo
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val currentBattery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val currentModel = "${Build.MANUFACTURER} ${Build.MODEL}"
            
            // Obtener ubicación si hay permisos
            var location = getLastKnownLocation()
            
            // Si no hay última ubicación conocida O la ubicación es muy antigua, intentar obtener una nueva
            val locationAge = location?.let { System.currentTimeMillis() - it.time } ?: Long.MAX_VALUE
            if (location == null || locationAge > 60000) { // Más de 1 minuto de antigüedad
                Log.d(TAG, "Location is null or too old (${locationAge}ms), requesting new location...")
                val newLocation = requestNewLocation()
                if (newLocation != null) {
                    location = newLocation
                }
            }
            
            // Verificar si hay cambios en el dispositivo
            var hasDeviceChanges = false
            if (device.model != currentModel || device.batteryLevel != currentBattery) {
                hasDeviceChanges = true
            }
            
            // Verificar si hay cambios en la ubicación
            val locationChanged = location != null && (
                location.latitude != device.latitude || 
                location.longitude != device.longitude
            )
            
            // Enviar heartbeat al servidor
            val response = remoteRepo.sendHeartbeat(
                deviceId = device.deviceId,
                latitude = location?.latitude,
                longitude = location?.longitude
            )
            
            if (response.isSuccessful) {
                Log.d(TAG, "Heartbeat sent successfully with location: ${location?.let { "lat=${it.latitude}, lon=${it.longitude}" } ?: "no location"}")
                
                // Actualizar información del dispositivo localmente
                val updatedDevice = device.copy(
                    model = currentModel,
                    batteryLevel = currentBattery,
                    lastSeen = System.currentTimeMillis(),
                    latitude = location?.latitude ?: device.latitude,
                    longitude = location?.longitude ?: device.longitude,
                    locationUpdatedAt = if (location != null) System.currentTimeMillis() else device.locationUpdatedAt
                )
                localRepo.updateDeviceInfo(updatedDevice)
                
                // Marcar para sincronización si hubo cambios
                if (hasDeviceChanges || locationChanged) {
                    syncHandler.markDeviceUpdatePending()
                    Log.d(TAG, "Device info changed (battery/model: $hasDeviceChanges, location: $locationChanged), marked for sync")
                }
            } else {
                Log.e(TAG, "Heartbeat failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending heartbeat", e)
        }
    }
    
    private fun getLastKnownLocation(): Location? {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permissions not granted")
            return null
        }
        
        // Verificar si el GPS está habilitado
        val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
        val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false
        
        Log.d(TAG, "GPS enabled: $isGpsEnabled, Network enabled: $isNetworkEnabled")
        
        if (!isGpsEnabled && !isNetworkEnabled) {
            Log.w(TAG, "No location providers enabled")
            return null
        }
        
        return try {
            // Intentar obtener la última ubicación conocida
            val gpsLocation = if (isGpsEnabled) {
                locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            } else null
            
            val networkLocation = if (isNetworkEnabled) {
                locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else null
            
            Log.d(TAG, "GPS Location: ${gpsLocation?.let { "lat=${it.latitude}, lon=${it.longitude}, age=${System.currentTimeMillis() - it.time}ms" } ?: "null"}")
            Log.d(TAG, "Network Location: ${networkLocation?.let { "lat=${it.latitude}, lon=${it.longitude}, age=${System.currentTimeMillis() - it.time}ms" } ?: "null"}")
            
            // Elegir la ubicación más reciente
            when {
                gpsLocation != null && networkLocation != null -> {
                    if (gpsLocation.time > networkLocation.time) gpsLocation else networkLocation
                }
                gpsLocation != null -> gpsLocation
                networkLocation != null -> networkLocation
                else -> {
                    Log.w(TAG, "No last known location available")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location", e)
            null
        }
    }
    
    // Método alternativo para solicitar ubicación activamente
    private suspend fun requestNewLocation(): Location? = suspendCoroutine { continuation ->
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            continuation.resume(null)
            return@suspendCoroutine
        }
        
        var isCompleted = false
        
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                Log.d(TAG, "New location received: lat=${location.latitude}, lon=${location.longitude}")
                locationManager?.removeUpdates(this)
                if (!isCompleted) {
                    isCompleted = true
                    continuation.resume(location)
                }
            }
            
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        
        try {
            // Intentar con GPS primero si está disponible
            val isGpsEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) ?: false
            val isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ?: false
            
            when {
                isGpsEnabled -> {
                    Log.d(TAG, "Requesting location update from GPS provider")
                    locationManager?.requestSingleUpdate(
                        LocationManager.GPS_PROVIDER,
                        locationListener,
                        Looper.getMainLooper()
                    )
                }
                isNetworkEnabled -> {
                    Log.d(TAG, "Requesting location update from Network provider")
                    locationManager?.requestSingleUpdate(
                        LocationManager.NETWORK_PROVIDER,
                        locationListener,
                        Looper.getMainLooper()
                    )
                }
                else -> {
                    Log.w(TAG, "No location providers available for update")
                    continuation.resume(null)
                    return@suspendCoroutine
                }
            }
            
            // Timeout después de 10 segundos
            serviceScope.launch {
                delay(10000)
                locationManager?.removeUpdates(locationListener)
                if (!isCompleted) {
                    isCompleted = true
                    Log.w(TAG, "Location request timed out")
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting location update", e)
            if (!isCompleted) {
                continuation.resume(null)
            }
        }
    }
} 