package com.ursolgleb.controlparental.utils

import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import java.util.Locale

object EventDetailsFormatter {

    fun getDetails(event: AccessibilityEvent): String {
        val eventType = getEventTypeName(event.eventType)
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
            📌 Detalles de la vista de origen:
            🖥 Clase: ${source.className}
            🏷 ID: $viewId
            🗣 Descripción: $contentDesc
            📐 Bounds: (${bounds.left}, ${bounds.top}, ${bounds.right}, ${bounds.bottom})
            """.trimIndent()
        } else {
            "📌 No hay información del nodo de accesibilidad."
        }

        return buildString {
            appendLine("🔹 Tipo de evento: $eventType")
            appendLine("📦 Paquete: $packageName")
            appendLine("🏷 Clase de origen: $className")
            appendLine("🖥 Vista donde ocurrió: $viewClass")
            appendLine("🏷 ID del elemento: $viewId")
            appendLine("🗣 Descripción del contenido: $contentDesc")
            appendLine("📝 Texto capturado: $eventText")
            appendLine("🌍 Idioma: $idioma")
            appendLine("🎮 Acción realizada: ${event.action}")
            appendLine(nodeInfo)
        }
    }

    private fun getEventTypeName(eventType: Int): String = when (eventType) {
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
        AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START -> "Inicio gesto exploración táctil"
        AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END -> "Fin gesto exploración táctil"
        AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> "Inicio interacción táctil"
        AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> "Fin interacción táctil"
        AccessibilityEvent.TYPE_GESTURE_DETECTION_START -> "Inicio detección de gestos"
        AccessibilityEvent.TYPE_GESTURE_DETECTION_END -> "Fin detección de gestos"
        AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> "Cambio en contenido ventana"
        AccessibilityEvent.TYPE_ANNOUNCEMENT -> "Anuncio de accesibilidad"
        AccessibilityEvent.TYPE_ASSIST_READING_CONTEXT -> "Asistencia lectura contexto"
        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED -> "Vista enfocada accesibilidad"
        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED -> "Enfoque accesibilidad eliminado"
        AccessibilityEvent.TYPE_VIEW_HOVER_ENTER -> "Cursor sobre vista"
        AccessibilityEvent.TYPE_VIEW_HOVER_EXIT -> "Cursor fuera vista"
        AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED -> "Clic contextual en vista"
        AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY -> "Texto recorrido con granularidad"
        else -> "Evento desconocido ($eventType)"
    }
}
