package util

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.ursolgleb.controlparental.utils.Launcher
import java.util.Locale


class Ulities : AccessibilityService() {

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
        TODO("Not yet implemented")
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    // Comparar permisos
    fun comparePermissions(context: Context) {
        val launcherPackage = Launcher.getDefaultLauncherPackageName(context)

        if (launcherPackage.isEmpty()) {
            Log.e("AppBlockerService", "La variable de launcher está vacía")
            return
        }

        val launcherPermissions = getPermissions(launcherPackage, context)
        val settingsPermissions = getPermissions("com.android.settings", context)
        val exclusiveLauncherPermissions = launcherPermissions - settingsPermissions
        Log.d(
            "AppBlockerService",
            "Permisos exclusivos del Launcher: $exclusiveLauncherPermissions"
        )
    }

    fun getPermissions(packageName: String, context: Context): List<String> {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo(
                packageName,
                PackageManager.GET_PERMISSIONS
            ).requestedPermissions?.toList() ?: emptyList()
        } catch (e: PackageManager.NameNotFoundException) {
            emptyList()
        }
    }

    fun accionesAccessibility() {
        performGlobalAction(GLOBAL_ACTION_BACK);          // Simula botón "Atrás"
        performGlobalAction(GLOBAL_ACTION_HOME);          // Simula botón "Inicio"
        performGlobalAction(GLOBAL_ACTION_RECENTS);       // Abre aplicaciones recientes
        performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS); // Abre la barra de notificaciones
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);// Abre ajustes rápidos
        performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);  // Muestra el menú de apagado
        performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN); // Activa/desactiva pantalla dividida
        performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);   // Bloquea la pantalla (Android 9+)
        performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);// Toma una captura de pantalla (Android 11+)
    }

    fun isRealLauncher(packageName: String, pm: PackageManager): Boolean {
        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            !appInfo.tieneFlagDeLauncher() // Excluir apps no relevantes
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    // Función para verificar si una app es de sistema
    fun ApplicationInfo.tieneFlagDeLauncher(): Boolean {
        return (flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }

    private fun getEventDetails(event: AccessibilityEvent) {
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
        val node = event.source  // Obtiene el nodo de accesibilidad
        val nodeInfo =  // Se define la variable nodeInfo y se le asigna un valor
            if (node != null) {  // Si el nodo no es nulo
                val bounds = Rect()
                node.getBoundsInScreen(bounds)  // Obtiene los límites de la vista en pantalla
                val boundsInfo =
                    "(${bounds.left}, ${bounds.top}, ${bounds.right}, ${bounds.bottom})"

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
                """.trimIndent()  // 📌 Aquí se asigna el valor a nodeInfo

            } else {
                "📌 No hay información del nodo de accesibilidad."  // Si node es nulo, se asigna este mensaje
            }


        // Construcción del mensaje con toda la información disponible
        // val detallesBase = """
        // 🔹 **Tipo de evento:** $eventType
        // 📦 **Paquete:** $packageName
        // 🏷 **Clase de origen:** $className
        // 🖥 **Vista donde ocurrió:** $viewClass
        // 🏷 **ID del elemento:** $viewId
        // 🗣 **Descripción del contenido:** $contentDesc
        // 📝 **Texto capturado:** $eventTextOriginal
        // 🌍 **Idioma:** $idioma
        // ⏳ **Tiempo del evento:** $eventTime
        // 📝 **Texto previo:** $beforeText
        // ✅ **¿Marcado?:** $isChecked
        // 🔒 **¿Campo de contraseña?:** $isPassword
        // 🔄 **¿Habilitado?:** $isEnabled
        // 🔳 **¿Pantalla completa?:** $isFullScreen
        // 🔽 **¿Scrollable?:** $isScrollable
        // 📜 **Scroll X/Y:** ($scrollX, $scrollY)
        // 📋 **Índice en lista:** $currentItemIndex / $itemCount
        // ➕ **Elementos agregados:** $addedCount
        // ➖ **Elementos eliminados:** $removedCount
        // 🎮 **Acción realizada:** $action
        // 📜 **Cantidad de registros:** $recordCount
        // 🔍 **Granularidad del movimiento:** $movementGranularity
        // 📨 **Datos adicionales:** $parcelableData
        // $nodeInfo
        // """.trimIndent() // (No se usa actualmente, comentar para evitar warning)

    }


    /*    // 🔥 ✅ Para agregar una app a la base de datos ⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰

    private val blockedAppsProcessing = mutableSetOf<String>()
    private val mutex_addApp = Mutex()

        fun addAppBlockAbd(packageName: String) {
            viewModelScope.launch(Dispatchers.IO) {

                mutex_addApp.withLock {
                    if (blockedAppsProcessing.contains(packageName)) {
                        Log.w("SharedViewModel1", "Ya se está procesando: $packageName")
                        return@launch
                    }
                    blockedAppsProcessing.add(packageName)
                }

                try {
                    val blockedDao = ControlParentalApp.dbApps.blockedDao()
                    val appDao = ControlParentalApp.dbApps.appDao()

                    val existingBlockedApp = blockedDao.getBlockedAppByPackageName(packageName)
                    if (existingBlockedApp != null) {
                        withContext(Dispatchers.Main) {
                            Log.w("SharedViewModel1", "App ya está bloqueada: $packageName")
                        }
                        return@launch
                    }

                    val existingAppEnSistema = appDao.getApp(packageName)
                    if (existingAppEnSistema == null) {
                        withContext(Dispatchers.Main) {
                            Log.w(
                                "SharedViewModel1",
                                "App no encontrada en bd de apps instaladas: $packageName"
                            )
                        }
                        return@launch
                    }

                    val newBlockedApp = BlockedEntity(packageName = packageName)
                    blockedDao.insertBlockedApp(newBlockedApp)

                    withContext(Dispatchers.Main) {
                        Log.w("SharedViewModel1", "Nueva App insertada a BLOCKED bd: $packageName")
                    }
                } catch (e: Exception) {
                    Log.e("SharedViewModel1", "Error al agregar app bloqueada: ${e.message}")
                } finally {
                    mutex_addApp.withLock {
                        blockedAppsProcessing.remove(packageName)
                    }
                }
            }
        }*/


}