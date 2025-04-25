package com.ursolgleb.controlparental.services

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.ursolgleb.controlparental.AppDataRepository

import com.ursolgleb.controlparental.LogDataRepository
import com.ursolgleb.controlparental.utils.Archivo
import java.util.Locale
import kotlinx.coroutines.SupervisorJob
import com.ursolgleb.controlparental.utils.AppsFun
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class AppBlockerService : AccessibilityService() {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    @Inject
    lateinit var logDataRepository: LogDataRepository

    // Mapa para almacenar los jobs en ejecución para cada paquete.
    private val packageJobsBlockNuevaApp = mutableMapOf<String, Job>()
    // Mutex para sincronizar el acceso al mapa y evitar condiciones de carrera.
    private val jobsBlockNuevaAppMutex = Mutex()

    private val packageJobsRenovarTiempoDeUsoHoy = mutableMapOf<String, Job>()
    private val jobsRenovarTiempoDeUsoHoyMutex = Mutex()
    // Mapa para almacenar el timestamp de la última ejecución para cada paquete.
    private val lastRenovacionTimeForPkg = mutableMapOf<String, Long>()
    // Mutex para sincronizar el acceso al mapa de tiempos.
    private val timeMutex = Mutex()

    private var isOnHomeScreen: Boolean? = null
    private var isBlockerEnabled = false
    private var currentPkg: String? = null

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val blockedWords = listOf("app", "aplicac")
    private val blockedWordsSub = listOf("app")

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        currentPkg = event.packageName?.toString() ?: return
        if (currentPkg == "com.ursolgleb.controlparental") return

        if (!::appDataRepository.isInitialized) return
        if (!::logDataRepository.isInitialized) return

        // 🔍 Captura detalles del evento para depuración
        //val eventDetales = getEventDetails(event)
        // putLog("$eventDetales\n")

        if (currentPkg != appDataRepository.defLauncher) {
            handleAppBlockedDetection()
            launchJobBlockNuevaApp(event)
            handleClickEvents(event)
            handleSubSettingsDetection(event)
        }

        launchJobRenovarTiempoDeUsoHoy(event)


        if (!isBlockerEnabled && (currentPkg == appDataRepository.defLauncher) != isOnHomeScreen) {
            isOnHomeScreen = currentPkg == appDataRepository.defLauncher
            putLog("¿Está en la pantalla de inicio? $isOnHomeScreen")
        }

        if (isBlockerEnabled) {
            if (currentPkg != appDataRepository.defLauncher) {
                putLog("❤️ Atras hasta Home: ${event.eventType}")
                performGlobalAction(GLOBAL_ACTION_HOME)
            } else {
                putLog("😶 LOCK_SCREEN 😶‍: ${event.eventType}")
                performGlobalAction(GLOBAL_ACTION_HOME)
                isBlockerEnabled = false
            }
        }
    }


    private fun handleAppBlockedDetection() {
        try {
            val pkg = currentPkg ?: return
            if (!isBlockerEnabled && isAppBlocked(pkg)) {
                isBlockerEnabled = true
                putBlockLog("❌ Bloqueada por lista de apps")
            }
        } catch (e: Exception) {
            Log.e("AppBlockerService", "Error en isAppBlocked: ${e.message}", e)
        }
    }


    private fun handleClickEvents(event: AccessibilityEvent) {
        if (!isBlockerEnabled &&
            event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED &&
            currentPkg == "com.android.settings"
        ) {
            if (shouldBlockAppByText(event, blockedWords)) {
                isBlockerEnabled = true
                putBlockLog("❌ Bloqueada por texto (settings)")
            }
        }
    }

    private fun handleSubSettingsDetection(event: AccessibilityEvent) {
        if (!isBlockerEnabled &&
            event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.className?.toString() == "com.android.settings.SubSettings"
        ) {
            if (shouldBlockAppByText(event, blockedWordsSub)) {
                isBlockerEnabled = true
                putBlockLog("❌ Bloqueada por texto (SubSettings)")
            }
        }
    }

    private fun launchJobRenovarTiempoDeUsoHoy(event: AccessibilityEvent) {
        if (isBlockerEnabled || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = currentPkg ?: return

        coroutineScope.launch {
            // Sincronizamos el acceso al mapa de tiempos y al mapa de jobs
            jobsRenovarTiempoDeUsoHoyMutex.withLock outer@{
                timeMutex.withLock {
                    val currentTime = System.currentTimeMillis()
                    val lastTime = lastRenovacionTimeForPkg[pkg] ?: 0L
                    // Si han pasado menos de 10 segundo, se omite lanzar el job
                    if (currentTime - lastTime < 10000L) return@outer
                    // Se actualiza el timestamp para el paquete
                    lastRenovacionTimeForPkg[pkg] = currentTime
                }
                // Verificar si ya existe un job activo para este paquete
                if (packageJobsRenovarTiempoDeUsoHoy[pkg]?.isActive == true) return@outer

                val job = launch {
                    try {
                        putLog("Primer plano: $currentPkg")
                        if (currentPkg in appDataRepository.todosAppsFlow.value.map { it.packageName }) {
                            appDataRepository.renovarTiempoUsoAppHoy(currentPkg!!)
                        }
                    } catch (e: Exception) {
                        Log.e("AppBlockerService", "Error en renovarTiempoUsoAppHoy: ${e.message}", e)
                    } finally {
                        jobsRenovarTiempoDeUsoHoyMutex.withLock {
                            packageJobsRenovarTiempoDeUsoHoy.remove(pkg)
                        }
                    }
                }
                packageJobsRenovarTiempoDeUsoHoy[pkg] = job
            }
        }
    }


    // En lugar de lanzar directamente la coroutine, lo hacemos de la siguiente forma:
    private fun launchJobBlockNuevaApp(event: AccessibilityEvent) {
        // Validamos que currentPkg no sea nulo
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = currentPkg ?: return

        // Usamos el mutex para sincronizar el acceso al mapa
        coroutineScope.launch {
            jobsBlockNuevaAppMutex.withLock {
                // Comprobamos si ya existe un job activo para este paquete
                if (packageJobsBlockNuevaApp[pkg]?.isActive == true) return@withLock
                // Si no existe, lanzamos un nuevo job y lo registramos en el mapa
                val job = launch {
                    try {
                        // Verificamos la condición: si no está activado el bloqueo y la app es nueva con UI
                        if (!isBlockerEnabled && isNewAppWithUi(pkg)) {
                            isBlockerEnabled = true
                            putBlockLog("👁️ App nueva con UI detectada")
                            appDataRepository.addNuevoPkgBD(pkg)
                        }
                    } catch (e: Exception) {
                        Log.e(
                            "AppBlockerService",
                            "Error en isNewAppWithUi o addNuevoPkgBD: ${e.message}",
                            e
                        )
                    } finally {
                        // Al finalizar el job, se elimina del mapa para permitir futuros lanzamientos para ese paquete.
                        jobsBlockNuevaAppMutex.withLock {
                            packageJobsBlockNuevaApp.remove(pkg)
                        }
                    }
                }
                // Se registra el job en el mapa para el paquete actual.
                packageJobsBlockNuevaApp[pkg] = job
            }
        }
    }

    private fun isAppBlocked(pkgName: String): Boolean {
        return appDataRepository.blockedAppsFlow.value.any { it.packageName == pkgName }
    }

    private suspend fun isNewAppWithUi(pkgName: String): Boolean {
        return appDataRepository.siEsNuevoPkg(pkgName) &&
                AppsFun.siTieneUI(this@AppBlockerService, pkgName)
    }

    private fun shouldBlockAppByText(
        event: AccessibilityEvent,
        blockedList: List<String>
    ): Boolean {
        val text = event.text.joinToString(", ")
        return blockedList.any { word -> text.contains(word, ignoreCase = true) }
    }

    private fun putBlockLog(msg: String) {
        val pkg = currentPkg ?: "Paquete desconocido"
        val fullMsg = "$msg: $pkg"
        coroutineScope.launch {
            try {
                Archivo.appendTextToFile(this@AppBlockerService, "\n $fullMsg")
            } catch (e: Exception) {
                Log.e("AppBlockerService", "Error al escribir en archivo: ${e.message}", e)
            }

            try {
                logDataRepository.saveLogBlockedApp(fullMsg)
            } catch (e: Exception) {
                Log.e("AppBlockerService", "Error al guardar log en BD: ${e.message}", e)
            }
            Log.e("AppBlockerService", fullMsg)
        }
    }


    private fun putLog(msg: String) {
        val pkg = currentPkg ?: "Paquete desconocido"
        val fullMsg = "$msg: $pkg"
        coroutineScope.launch {
            try {
                Archivo.appendTextToFile(this@AppBlockerService, "\n $fullMsg")
            } catch (e: Exception) {
                Log.e("AppBlockerService", "Error al escribir log en archivo: ${e.message}", e)
            }
            Log.e("AppBlockerService", fullMsg)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }


    override fun onInterrupt() {
        putLog("Servicio de accesibilidad interrumpido")
    }


    private fun getEventDetails(event: AccessibilityEvent): String {
        val eventType = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> "Vista clicada"
            AccessibilityEvent.TYPE_VIEW_LONG_CLICKED -> "Vista con clic largo"
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> "Vista enfocada"
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> "Texto cambiado"
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "Cambio de ventana"
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> "Notificación cambiada"
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> "Vista desplazada"
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "Cambio en ventanas"
            AccessibilityEvent.TYPE_VIEW_SELECTED -> "Vista seleccionada"
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> "Selección de texto"
            AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START -> "Inicio de gesto de exploración táctil"
            AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END -> "Fin de gesto de exploración táctil"
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> "Inicio de interacción táctil"
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> "Fin de interacción táctil"
            AccessibilityEvent.TYPE_GESTURE_DETECTION_START -> "Inicio de detección de gestos"
            AccessibilityEvent.TYPE_GESTURE_DETECTION_END -> "Fin de detección de gestos"
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "Cambio en contenido de ventana"
            AccessibilityEvent.TYPE_ANNOUNCEMENT -> "Anuncio de accesibilidad"
            AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT -> "Asistencia en lectura de contexto"
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> "Vista enfocada en accesibilidad"
            AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED -> "Enfoque de accesibilidad eliminado"
            AccessibilityEvent.TYPE_VIEW_HOVER_ENTER -> "Cursor sobre vista"
            AccessibilityEvent.TYPE_VIEW_HOVER_EXIT -> "Cursor fuera de vista"
            AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED -> "Clic contextual en vista"
            AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY -> "Texto recorrido con granularidad de movimiento"
            else -> "Evento desconocido (${event.eventType})"
        }

        val packageName = event.packageName?.toString() ?: "Desconocido"
        val className = event.className?.toString() ?: "Desconocido"
        val eventText = event.text.joinToString(", ").ifEmpty { "Sin texto" }
        val idioma = Locale.getDefault().language

        val source = event.source
        val viewClass = source?.className?.toString() ?: "Desconocido"
        val viewId = source?.viewIdResourceName ?: "ID no disponible"
        val contentDesc = source?.contentDescription?.toString() ?: "Sin descripción"

        val nodeInfo = if (source != null) {
            val bounds = Rect().apply { source.getBoundsInScreen(this) }
            """
        📌 **Detalles de la vista de origen:**
        🖥 **Clase:** ${source.className}
        🏷 **ID:** ${viewId}
        🗣 **Descripción del contenido:** ${contentDesc}
        📐 **Bounds:** (${bounds.left}, ${bounds.top}, ${bounds.right}, ${bounds.bottom})
        """.trimIndent()
        } else {
            "📌 No hay información del nodo de accesibilidad."
        }

        return buildString {
            appendLine("🔹 **Tipo de evento:** $eventType")
            appendLine("📦 **Paquete:** $packageName")
            appendLine("🏷 **Clase de origen:** $className")
            appendLine("🖥 **Vista donde ocurrió:** $viewClass")
            appendLine("🏷 **ID del elemento:** $viewId")
            appendLine("🗣 **Descripción del contenido:** $contentDesc")
            appendLine("📝 **Texto capturado:** $eventText")
            appendLine("🌍 **Idioma:** $idioma")
            appendLine("🎮 **Acción realizada:** ${event.action}")
            appendLine(nodeInfo)
        }
    }


}

