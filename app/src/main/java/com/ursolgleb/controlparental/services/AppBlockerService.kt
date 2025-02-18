package com.ursolgleb.controlparental.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast

import com.ursolgleb.controlparental.ControlParentalApp
import com.ursolgleb.controlparental.utils.Archivo
import com.ursolgleb.controlparental.utils.Launcher
import java.util.Locale
import com.ursolgleb.controlparental.UI.activities.DesarolloActivity
import com.ursolgleb.controlparental.allowedApps
import com.ursolgleb.controlparental.data.local.AppDatabase
import com.ursolgleb.controlparental.data.log.LogBlockedAppEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AppBlockerService : AccessibilityService() {

    @Inject
    lateinit var appDatabase: AppDatabase

    val blockedDao = appDatabase.blockedDao()

    private var isBlockerEnabled = false
    private var currentAppEnPrimerPlano: String? = null
    val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val blockedWords = listOf(
        "app"
    )
    private val blockedWordsSub = listOf(
        "app"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (event == null) return

        if (event.packageName == "com.ursolgleb.controlparental"
        //&& event.className != "android.widget.TextView"
        ) {
            return
        }

        val eventDetales = getEventDetails(event)
        Log.d("AppBlockerService", eventDetales)
        Archivo.appendTextToFile(this, DesarolloActivity.fileName, "\n $eventDetales")

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            currentAppEnPrimerPlano = packageName
            blockearSiEnBlackList(packageName)

            val msg = "App en primer plano: $packageName"
            Log.w("AppBlockerService111", msg)
            Archivo.appendTextToFile(this, DesarolloActivity.fileName, "\n $msg")

            //base de datos renovar una app
        }

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val packageName = event.packageName?.toString() ?: return
            currentAppEnPrimerPlano = packageName
            blockearSiEnBlackList(packageName)

            if (packageName == "com.android.settings") {
                getEventTextoCapturado(event) { textoCapturado ->
                    for (blockedWord in blockedWords) {
                        if (textoCapturado.contains(blockedWord, ignoreCase = true)
                        ) {
                            performGlobalAction(GLOBAL_ACTION_BACK)   // Simula botón "Atrás"
                            isBlockerEnabled = true
                            saveBlockedAppBaseDeDatos("$packageName, ❌: $blockedWord")
                            val msg =
                                "Bloqueando app: $packageName, porque contiene la palabra: $blockedWord"
                            Log.e("AppBlockerService", msg)
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                            Archivo.appendTextToFile(this, DesarolloActivity.fileName, "\n $msg")
                            return@getEventTextoCapturado
                        }
                    }
                }
            }
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val claseDeOrigen = event.className?.toString() ?: return
            if (claseDeOrigen == "com.android.settings.SubSettings") {
                getEventTextoCapturado(event) { textoCapturado ->
                    for (blockedWordsSub in blockedWordsSub) {
                        if (textoCapturado.contains(blockedWordsSub, ignoreCase = true)) {
                            performGlobalAction(GLOBAL_ACTION_BACK)
                            isBlockerEnabled = true
                            saveBlockedAppBaseDeDatos("$claseDeOrigen, ❌: $blockedWordsSub")
                            val msg =
                                "Bloqueando 555 app: $claseDeOrigen, porque contiene la palabra: $blockedWordsSub"
                            Log.e("AppBlockerService", msg)
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                            Archivo.appendTextToFile(this, DesarolloActivity.fileName, "\n $msg")
                            return@getEventTextoCapturado
                        }
                    }
                }
            }
        }

        if (isBlockerEnabled) {
            if (Launcher.getDefaultLauncherPackageName(this) != event.packageName) {
                val msg = "❤️❤️❤️Atras hasta Home 555: ${event.eventType}"
                Log.w("AppBlockerService", msg)
                Archivo.appendTextToFile(this, DesarolloActivity.fileName, "\n $msg")
                performGlobalAction(GLOBAL_ACTION_BACK)
            } else {
                val msg = "😶‍🌫️😶‍🌫️😶‍🌫️LOCK_SCREEN😶‍🌫️😶‍🌫️😶‍🌫️: ${event.eventType}"
                Log.w("AppBlockerService", msg)
                Archivo.appendTextToFile(this, DesarolloActivity.fileName, "\n $msg")
                //performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                isBlockerEnabled = false
            }
        }

        val isOnHomeScreen =
            Launcher.getDefaultLauncherPackageName(this) == event.packageName
        val msg = "Está 888 en la pantalla de inicio? $isOnHomeScreen"
        Log.w("AppBlockerService", msg)
        Archivo.appendTextToFile(this, DesarolloActivity.fileName, "\n $msg")

    }

    private fun blockearSiEnBlackList(packageName: String) {
        coroutineScope.launch {
            val blockedApp = blockedDao.getBlockedAppByPackageName(packageName)
            if (blockedApp == null) {
                Log.w("AppBlockerService111", "blockedApp == null true")
                return@launch
            } else {
                Log.w("AppBlockerService111", "blockedApp == null false")
                performGlobalAction(GLOBAL_ACTION_BACK)
                isBlockerEnabled = true
                saveBlockedAppBaseDeDatos(packageName)
                val msg = "Bloqueando app: $packageName"
                Log.e("AppBlockerService111", msg)
                Archivo.appendTextToFile(
                    this@AppBlockerService,
                    DesarolloActivity.fileName,
                    "\n $msg"
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AppBlockerService, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        allowedApps.apps.add(Launcher.getDefaultLauncherPackageName(this))

        allowedApps.showApps(this)

        val msg = "Servicio de accesibilidad iniciado"
        Log.w("AppBlockerService", msg)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        Archivo.appendTextToFile(this, DesarolloActivity.fileName, "\n $msg")

    }


    override fun onInterrupt() {
        val msg = "Servicio de accesibilidad interrumpido"
        Log.w("AppBlockerService", msg)
        Archivo.appendTextToFile(this, DesarolloActivity.fileName, "\n $msg")
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
        val viewClass = event.source?.className?.toString() ?: "Desconocido"
        val viewId = event.source?.viewIdResourceName ?: "ID no disponible"
        val contentDesc = event.source?.contentDescription?.toString() ?: "Sin descripción"
        val eventTextOriginal = event.text.joinToString(", ") ?: "Sin texto"
        val idioma = Locale.getDefault().language

        // Información avanzada del evento
        val eventTime = event.eventTime
        val beforeText = event.beforeText?.toString() ?: "No disponible"
        val isChecked = event.isChecked
        val isPassword = event.isPassword
        val isEnabled = event.isEnabled
        val isFullScreen = event.isFullScreen
        val isScrollable = event.isScrollable
        val scrollX = event.scrollX
        val scrollY = event.scrollY
        val itemCount = event.itemCount
        val currentItemIndex = event.currentItemIndex
        val addedCount = event.addedCount
        val removedCount = event.removedCount
        val action = event.action
        val recordCount = event.recordCount
        val movementGranularity = event.movementGranularity
        val parcelableData = event.parcelableData?.toString() ?: "No disponible"

        // Información detallada del nodo de accesibilidad
        val node = event.source
        val nodeInfo = if (node != null) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            val boundsInfo = "(${bounds.left}, ${bounds.top}, ${bounds.right}, ${bounds.bottom})"
            """
            📌 **Detalles de la vista de origen:**
            🖥 **Clase:** ${node.className}
            🏷 **ID:** ${node.viewIdResourceName ?: "No disponible"}
            🗣 **Descripción del contenido:** ${node.contentDescription ?: "Sin descripción"}
            """.trimIndent()
        } else {
            "📌 No hay información del nodo de accesibilidad."
        }

        // Construcción del mensaje con toda la información disponible
        val detallesBase = """
            
        🔹 **Tipo de evento:** $eventType
        📦 **Paquete:** $packageName
        🏷 **Clase de origen:** $className
        🖥 **Vista donde ocurrió:** $viewClass
        🏷 **ID del elemento:** $viewId
        🗣 **Descripción del contenido:** $contentDesc
        📝 **Texto capturado:** $eventTextOriginal
        🌍 **Idioma:** $idioma
        🎮 **Acción realizada:** $action
        $nodeInfo
        
    """.trimIndent()

        return detallesBase
    }


    private fun getEventTextoCapturado(
        event: AccessibilityEvent,
        onResult: (String) -> Unit
    ) {
        val eventTextOriginal = event.text.joinToString(", ")
        val textoCapturado = eventTextOriginal.trimIndent()
        onResult(textoCapturado)
    }

    private fun saveBlockedAppBaseDeDatos(packageName: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val existingApp =
                ControlParentalApp.dbLogs.logBlockedAppDao().getLogBlockedApp(packageName)

            if (existingApp != null) {
                // Si el packageName ya existe, actualiza el campo blockedAt
                val updatedApp = existingApp.copy(blockedAt = System.currentTimeMillis())
                ControlParentalApp.dbLogs.logBlockedAppDao().updateLogBlockedApp(updatedApp)
            } else {
                // Si no existe, inserta un nuevo registro
                ControlParentalApp.dbLogs.logBlockedAppDao()
                    .insertLogBlockedApp(LogBlockedAppEntity(packageName = packageName))
            }
            withContext(Dispatchers.Main) {
                val intent = Intent("com.ursolgleb.controlparental.UPDATE_BLOCKED_APPS")
                sendBroadcast(intent)
                Log.w("MainActivityListaApps", "Broadcast enviado desde saveBlockedAppBaseDeDatos")
            }
        }
    }

}
