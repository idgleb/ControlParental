package com.ursolgleb.controlparental.services


import kotlinx.coroutines.*

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.ursolgleb.controlparental.UI.activities.AuthActivity
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.handlers.AppBlockHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject


@AndroidEntryPoint
class AppBlockerService : AccessibilityService() {

    @Inject
    lateinit var appBlockHandler: AppBlockHandler
    @Inject
    lateinit var appDataRepository: AppDataRepository

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 1. declarar
    private val authReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            if (i?.getBooleanExtra("ok", false) == true) {
                appBlockHandler.resetBlockFlag()   // desbloquea
            }
        }
    }

    // 2. registrar
    override fun onServiceConnected() {
        super.onServiceConnected()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(authReceiver, IntentFilter("AUTH_RESULT"))
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        appDataRepository.currentPkg = event.packageName?.toString() ?: return

        // 1. NO bloquees si estás dentro de tu AuthActivity
        if (appDataRepository.currentPkg == this.packageName) return

        appBlockHandler.handle(event, appDataRepository.currentPkg ?: return)

        if (appBlockHandler.isBlocking) {
            if (appDataRepository.currentPkg != appDataRepository.defLauncher) {
                appBlockHandler.log(
                    "🔴 Ejecutando GLOBAL_ACTION_HOME (x2)",
                    appDataRepository.currentPkg!!
                )
                performGlobalAction(GLOBAL_ACTION_HOME)

                coroutineScope.launch {
                    delay(500)
                    if (appDataRepository.currentPkg != appDataRepository.defLauncher) {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                    }
                }

            } else {
                appBlockHandler.log(
                    "🟢 En launcher, reseteando bloqueo",
                    appDataRepository.currentPkg!!
                )
                //performGlobalAction(GLOBAL_ACTION_HOME)
                appBlockHandler.resetBlockFlag()
                coroutineScope.launch {
                    delay(500)
                    appBlockHandler.log(
                        "🏁 showAuthenticationDialog",
                        appDataRepository.currentPkg!!
                    )
                    showAuthenticationDialog()
                }
            }
        }

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
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(authReceiver)
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
