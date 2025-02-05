package com.ursolgleb.controlparental

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale
import android.provider.Settings

class AppBlockerService : AccessibilityService() {

    private lateinit var allowedApps: MutableList<String>

    private var isBlockerEnabled = false

    private val blockedWords = listOf(
        "app"
    )
    private val blockedWordsSub = listOf(
        "app"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        if (event == null) return

        Log.e("AppBlockerService", "Home page App: ${Launcher.getDefaultLauncherPackageName(this)}")

        getEventDetails(event) { detallesTraducidos ->
            Log.d("AppBlockerService", detallesTraducidos)
        }

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            Log.w("AppBlockerService", "App en primer plano: $packageName")
            Toast.makeText(this, "App en primer plano: $packageName", Toast.LENGTH_SHORT).show()

            if (!allowedApps.contains(packageName)) {
                performGlobalAction(GLOBAL_ACTION_BACK)
                isBlockerEnabled = true
                Log.e("AppBlockerService", "Bloqueando app: $packageName")
                Toast.makeText(this, "$packageName está bloqueada", Toast.LENGTH_SHORT).show()
            }
        }

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            val packageName = event.packageName?.toString() ?: return
            if (packageName == "com.android.settings") {
                getEventTextoCapturado(event) { textoCapturado ->
                    for (blockedWord in blockedWords) {
                        if (textoCapturado.contains("app", ignoreCase = true) &&
                            textoCapturado.contains("Assistant", ignoreCase = true)
                        ) {
                            performGlobalAction(GLOBAL_ACTION_BACK)       // Simula botón "Atrás"
                            isBlockerEnabled = true
                            Log.e(
                                "AppBlockerService",
                                "Bloqueando app: $packageName, porque contiene la palabra: $blockedWord"
                            )
                            Toast.makeText(
                                this,
                                "$packageName está bloqueada, porque contiene la palabra: $blockedWord",
                                Toast.LENGTH_SHORT
                            )
                                .show()
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
                            Log.e(
                                "AppBlockerService",
                                "Bloqueando 555 app: $claseDeOrigen, porque contiene la palabra: $blockedWordsSub"
                            )
                            Toast.makeText(
                                this,
                                "$claseDeOrigen está bloqueada, porque contiene la palabra: $blockedWordsSub",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            return@getEventTextoCapturado
                        }
                    }
                }
            }
        }

        if (isBlockerEnabled) {
            if (Launcher.getDefaultLauncherPackageName(this) != event.packageName) {
                Log.w("AppBlockerService", "Atras hasta Home 555: ${event.eventType}")
                performGlobalAction(GLOBAL_ACTION_BACK)
            } else {
                Log.w("AppBlockerService", "LOCK_SCREEN: ${event.eventType}")
                //performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                isBlockerEnabled = false
            }
        }

        val isOnHomeScreen =
            Launcher.getDefaultLauncherPackageName(this) == event.packageName
        Log.w("AppBlockerService", "¿Está 888 en la pantalla de inicio? $isOnHomeScreen")

    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        allowedApps = mutableListOf(
            "com.ursolgleb.appblocker",
            "com.ursolgleb.controlparental",
            "com.android.chrome",
            "com.google.android.apps.nexuslauncher",
            "com.android.settings",
            "com.android.systemui",
            "com.google.android.inputmethod.latin"
        )

        allowedApps.add(Launcher.getDefaultLauncherPackageName(this))

        Log.d("AppBlockerService", "Lista de aplicaciones acceptadas:")
        allowedApps.forEach { Log.w("AppBlockerService", it) }

        Toast.makeText(this, "Servicio de accesibilidad iniciado", Toast.LENGTH_SHORT).show()
    }


    override fun onInterrupt() {
        Log.w("AppBlockerService", "Servicio de accesibilidad interrumpido")
    }


    private fun getEventDetails(
        event: AccessibilityEvent,
        onResult: (String) -> Unit
    ) {
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
        🔘 **Clickable:** ${node.isClickable}
        🔘 **LongClickable:** ${node.isLongClickable}
        🔘 **Focusable:** ${node.isFocusable}
        🔘 **Focused:** ${node.isFocused}
        🔘 **Selected:** ${node.isSelected}
        👁 **Visible al usuario:** ${node.isVisibleToUser}
        🔲 **Bounds:** $boundsInfo
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
        ⏳ **Tiempo del evento:** $eventTime
        📝 **Texto previo:** $beforeText
        ✅ **¿Marcado?:** $isChecked
        🔒 **¿Campo de contraseña?:** $isPassword
        🔄 **¿Habilitado?:** $isEnabled
        🔳 **¿Pantalla completa?:** $isFullScreen
        🔽 **¿Scrollable?:** $isScrollable
        📜 **Scroll X/Y:** ($scrollX, $scrollY)
        📋 **Índice en lista:** $currentItemIndex / $itemCount
        ➕ **Elementos agregados:** $addedCount
        ➖ **Elementos eliminados:** $removedCount
        🎮 **Acción realizada:** $action
        📜 **Cantidad de registros:** $recordCount
        🔍 **Granularidad del movimiento:** $movementGranularity
        📨 **Datos adicionales:** $parcelableData
        $nodeInfo
    """.trimIndent()

        // Si el idioma no es inglés, traducir el texto; de lo contrario, usarlo tal cual.
        if (idioma != "en" && eventTextOriginal.isNotEmpty()) {
            traducirTexto(eventTextOriginal, idioma, "en") { textoTraducido ->
                val detallesTraducidos = detallesBase.replace(eventTextOriginal, textoTraducido)
                onResult(detallesTraducidos)
            }
        } else {
            onResult(detallesBase)
        }
    }


    private fun getEventTextoCapturado(
        event: AccessibilityEvent,
        onResult: (String) -> Unit
    ) {
        val eventTextOriginal = event.text.joinToString(", ")
        val idioma = Locale.getDefault().language

        // Si el idioma no es inglés, traducir el texto; de lo contrario, usarlo tal cual.
        if (idioma != "en" && eventTextOriginal != "") {
            traducirTexto(eventTextOriginal, idioma, "en") { textoTraducido ->
                val textoCapturado = textoTraducido.trimIndent()
                onResult(textoCapturado)
            }
        } else {
            val textoCapturado = eventTextOriginal.trimIndent()
            onResult(textoCapturado)
        }
    }


    fun traducirTexto(
        texto: String,
        idiomaOrigen: String,
        idiomaDestino: String = "en",
        resultado: (String) -> Unit
    ) {

        val mlKitIdiomaOrigen =
            TranslateLanguage.fromLanguageTag(idiomaOrigen) ?: TranslateLanguage.ENGLISH

        val mlKitIdiomaDestino =
            TranslateLanguage.fromLanguageTag(idiomaDestino) ?: TranslateLanguage.ENGLISH

        val opciones = TranslatorOptions.Builder()
            .setSourceLanguage(mlKitIdiomaOrigen)
            .setTargetLanguage(mlKitIdiomaDestino)
            .build()
        val traductor = Translation.getClient(opciones)

        // Configurar condiciones para la descarga del modelo (por ejemplo, solo en Wi-Fi)
        val condiciones = DownloadConditions.Builder().build()

        // Descargar el modelo si es necesario y luego traducir el texto
        traductor.downloadModelIfNeeded(condiciones)
            .addOnSuccessListener {
                traductor.translate(texto)
                    .addOnSuccessListener { textoTraducido ->
                        resultado(textoTraducido)
                    }
                    .addOnFailureListener { e ->
                        resultado("Error al traducir: ${e.localizedMessage}")
                    }
            }
            .addOnFailureListener { e ->
                resultado("Error al descargar el modelo: ${e.localizedMessage}")
            }
    }


}
