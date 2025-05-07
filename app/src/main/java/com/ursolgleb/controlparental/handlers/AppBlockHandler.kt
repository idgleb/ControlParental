package com.ursolgleb.controlparental.handlers

import android.view.accessibility.AccessibilityEvent
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.data.log.LogDataRepository
import com.ursolgleb.controlparental.checkers.AppBlockChecker
import com.ursolgleb.controlparental.checkers.HorarioBlockChecker
import com.ursolgleb.controlparental.checkers.TiempoUsoBlockChecker
import com.ursolgleb.controlparental.detectors.SettingsClickDetector
import com.ursolgleb.controlparental.detectors.SubSettingsDetector
import com.ursolgleb.controlparental.utils.Logger
import com.ursolgleb.controlparental.utils.PerPackageJobManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppBlockHandler @Inject constructor(
    val appDataRepository: AppDataRepository,
    private val logDataRepository: LogDataRepository,
    private val appBlockChecker: AppBlockChecker,
    private val horarioBlockChecker: HorarioBlockChecker,
    private val tiempoUsoBlockChecker: TiempoUsoBlockChecker,
    private val settingsClickDetector: SettingsClickDetector,
    private val subSettingsDetector: SubSettingsDetector
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val jobManager = PerPackageJobManager()
    private val lastEventTime = mutableMapOf<String, Long>()

    private var isBlockerEnabled = false
    val isBlocking: Boolean
        get() = isBlockerEnabled

    fun resetBlockFlag() {
        isBlockerEnabled = false
    }

    fun handle(event: AccessibilityEvent, packageName: String) {
        if (packageName == "com.ursolgleb.controlparental") return

        if (packageName != appDataRepository.defLauncher) {
            handleAppBlockedDetection(packageName)
            launchJobBlockNuevaApp(event, packageName)
            handleClickEvents(event, packageName)
            handleSubSettingsDetection(event)
        }

        launchJobRenovarTiempo(packageName, event)
    }

    private fun handleAppBlockedDetection(pkg: String) {
        try {
            when {
                appBlockChecker.isAppBlocked(pkg) -> {
                    isBlockerEnabled = true
                    logBlocked("❌ Bloqueada por lista de apps", pkg)
                }
                horarioBlockChecker.shouldBlock(pkg) -> {
                    isBlockerEnabled = true
                    logBlocked("⏰ Bloqueada por horario activo", pkg)
                }
                tiempoUsoBlockChecker.shouldBlock(pkg) -> {
                    isBlockerEnabled = true
                    logBlocked("🕒 Bloqueada por límite de tiempo diario", pkg)
                }
            }
        } catch (e: Exception) {
            Logger.error(appDataRepository.context, "AppBlockHandler", "Error en bloqueos: ${e.message}", e)
        }
    }

    private fun handleClickEvents(event: AccessibilityEvent, pkg: String) {
        if (!isBlockerEnabled && settingsClickDetector.shouldBlock(event, pkg)) {
            isBlockerEnabled = true
            logBlocked("❌ Bloqueada por texto (settings)", pkg)
        }
    }

    private fun handleSubSettingsDetection(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (!isBlockerEnabled && subSettingsDetector.shouldBlock(event)) {
            isBlockerEnabled = true
            logBlocked("❌ Bloqueada por texto (SubSettings)", pkg)
        }
    }

    private fun launchJobRenovarTiempo(pkg: String, event: AccessibilityEvent) {
        if (isBlockerEnabled || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val now = System.currentTimeMillis()
        val lastTime = lastEventTime[pkg] ?: 0L
        if (now - lastTime < 10000L) return
        lastEventTime[pkg] = now

        coroutineScope.launch {
            jobManager.launchUniqueJob(coroutineScope, "renovar_$pkg") {
                try {
                    if (pkg in appDataRepository.todosAppsFlow.value.map { it.packageName }) {
                        appDataRepository.updateTiempoDeUsoUnaApp(pkg)
                    }
                } catch (e: Exception) {
                    Logger.error(appDataRepository.context, "AppBlockHandler", "Error en renovarTiempoUsoAppHoy: ${e.message}", e)
                }
            }
        }
    }

    private fun launchJobBlockNuevaApp(event: AccessibilityEvent, pkg: String) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        coroutineScope.launch {
            jobManager.launchUniqueJob(coroutineScope, "bloqueo_$pkg") {
                try {
                    if (!isBlockerEnabled && appBlockChecker.isNewAppWithUi(pkg)) {
                        isBlockerEnabled = true
                        logBlocked("👁️ App nueva con UI detectada", pkg)
                        appDataRepository.addNuevoPkgBD(pkg)
                    }
                } catch (e: Exception) {
                    Logger.error(appDataRepository.context, "AppBlockHandler", "Error en isNewAppWithUi o addNuevoPkgBD: ${e.message}", e)
                }
            }
        }
    }

    private fun logBlocked(message: String, pkg: String) {
        coroutineScope.launch {
            val fullMessage = "$message: $pkg"
            try {
                Logger.info(appDataRepository.context, "AppBlockHandler", fullMessage)
                logDataRepository.saveLogBlockedApp(pkg)
            } catch (e: Exception) {
                Logger.error(appDataRepository.context, "AppBlockHandler", "Error al registrar bloqueo: ${e.message}", e)
            }
        }
    }

    fun log(message: String, pkg: String) {
        Logger.info(appDataRepository.context, "AppBlockHandler", "$message: $pkg")
    }

}
