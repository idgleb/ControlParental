package com.ursolgleb.controlparental.services


import kotlinx.coroutines.*

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ursolgleb.controlparental.UI.activities.AuthActivity
import com.ursolgleb.controlparental.data.local.AppDataRepository
import com.ursolgleb.controlparental.handlers.AppBlockHandler
import com.ursolgleb.controlparental.utils.Session
import com.ursolgleb.controlparental.services.LocationWatcherService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject


@AndroidEntryPoint
class AppBlockerService : AccessibilityService() {

    @Inject lateinit var appBlockHandler: AppBlockHandler
    @Inject lateinit var appDataRepository: AppDataRepository
    @Inject lateinit var session: Session

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        Log.d("AppBlockerService333", "onAccessibilityEvent: ${event?.eventType}, ${event?.text}")

        if (session.isSessionActive()) return

        if (event == null) return

        appDataRepository.currentPkg = event.packageName?.toString() ?: return

        // 1. NO bloquees si estás dentro de tu AuthActivity
        //if (appDataRepository.currentPkg == this.packageName) return

        appBlockHandler.handle(event)

        if (appBlockHandler.isBlocking) {
            appBlockHandler.log(
                "🔴 Ejecutando GLOBAL_ACTION_BACK o GLOBAL_ACTION_HOME",
                appDataRepository.currentPkg!!
            )
            //performGlobalAction(GLOBAL_ACTION_BACK)
            appBlockHandler.log(
                "🏁 showAuthenticationDialog",
                appDataRepository.currentPkg!!
            )
            Log.d("AppBlockerService333", "currentPkg=${appDataRepository.currentPkg}")
            Log.d("AppBlockerService333", "showAuthenticationDialog")
            appBlockHandler.resetBlockFlag()
            showAuthenticationDialog()
            Log.d("AppBlockerService333", "currentPkg2=${appDataRepository.currentPkg}")
        }

    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (hasLocationPermission()) {
            LocationWatcherService.start(this)
            Log.d("AppBlockerService", "LocationWatcherService arrancado desde AccessibilityService")
        } else {
            Log.w("AppBlockerService", "No hay permisos de ubicación, no se arranca LocationWatcherService")
            // Aquí podrías notificar a la UI principal para pedir permisos si lo deseas
        }
    }

    private fun hasLocationPermission(): Boolean {
        return checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
               checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun showAuthenticationDialog() {
        Handler(Looper.getMainLooper()).post {
            startActivity(
                Intent(this, AuthActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP
                    )
                }
            )
        }
    }


    override fun onDestroy() {
        coroutineScope.cancel()
        super.onDestroy()
    }

    override fun onInterrupt() {
        appBlockHandler.log(
            "Servicio de accesibilidad interrumpido",
            appDataRepository.currentPkg ?: ""
        )
    }
}
