package com.ursolgleb.controlparental.services

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object ServiceStarter {
    fun startBackgroundServices(context: Context) {
        // Iniciar HeartbeatService siempre
        HeartbeatService.start(context)
        // Iniciar LocationWatcherService solo si hay permisos
        val hasLocationPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasLocationPermission) {
            LocationWatcherService.start(context)
        }
    }
    fun stopBackgroundServices(context: Context) {
        HeartbeatService.stop(context)
        LocationWatcherService.stop(context)
    }
} 