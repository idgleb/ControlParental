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

    @Inject lateinit var appBlockHandler: AppBlockHandler //
    @Inject lateinit var appDataRepository: AppDataRepository//

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var currentPkg: String? = null
    private var isOnHomeScreen: Boolean? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        currentPkg = event.packageName?.toString() ?: return

        appBlockHandler.handle(event, currentPkg ?: return)

        if (!appBlockHandler.isBlocking && (currentPkg == appDataRepository.defLauncher) != isOnHomeScreen) {
            isOnHomeScreen = currentPkg == appDataRepository.defLauncher
            appBlockHandler.log("¿Está en la pantalla de inicio? $isOnHomeScreen", currentPkg!!)
        }

        if (appBlockHandler.isBlocking) {
            if (currentPkg != appDataRepository.defLauncher) {
                appBlockHandler.log("🔴 Ejecutando GLOBAL_ACTION_HOME (x2)", currentPkg!!)
                performGlobalAction(GLOBAL_ACTION_HOME)
                coroutineScope.launch {
                    delay(500)
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            } else {
                appBlockHandler.log("🟢 En launcher, reseteando bloqueo", currentPkg!!)
                performGlobalAction(GLOBAL_ACTION_HOME)
                appBlockHandler.resetBlockFlag()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    override fun onInterrupt() {
        appBlockHandler.log("Servicio de accesibilidad interrumpido", currentPkg ?: "")
    }
}
