package util

import android.accessibilityservice.AccessibilityService
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent


class Ulities: AccessibilityService() {

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
        TODO("Not yet implemented")
    }

    override fun onInterrupt() {
        TODO("Not yet implemented")
    }

    fun accionesAccessibility(){
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



}