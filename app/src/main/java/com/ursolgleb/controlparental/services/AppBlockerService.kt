package com.ursolgleb.controlparental.services

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.handlers.AppBlockHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*

import javax.inject.Inject

@AndroidEntryPoint
class AppBlockerService : AccessibilityService() {

    @Inject lateinit var appBlockHandler: AppBlockHandler
    @Inject lateinit var appDataRepository: AppDataRepository

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        appDataRepository.currentPkg = event.packageName?.toString() ?: return

        appBlockHandler.handle(event, appDataRepository.currentPkg ?: return)

        if (appBlockHandler.isBlocking) {
            if (appDataRepository.currentPkg != appDataRepository.defLauncher) {
                appBlockHandler.log("🔴 Ejecutando GLOBAL_ACTION_HOME (x2)", appDataRepository.currentPkg!!)
                performGlobalAction(GLOBAL_ACTION_HOME)
                coroutineScope.launch {
                    delay(500)
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    showAuthenticationDialog()
                }
            } else {
                appBlockHandler.log("🟢 En launcher, reseteando bloqueo", appDataRepository.currentPkg!!)
                performGlobalAction(GLOBAL_ACTION_HOME)
                appBlockHandler.resetBlockFlag()
            }
        }

    }

    private fun showAuthenticationDialog() {
        // Aquí debes implementar el diálogo de autenticación (PIN, biometría, etc)
        // Solo permite que continúe si la autenticación es correcta
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    override fun onInterrupt() {
        appBlockHandler.log("Servicio de accesibilidad interrumpido", appDataRepository.currentPkg ?: "")
    }
}
