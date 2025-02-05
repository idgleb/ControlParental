package util

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.ursolgleb.controlparental.Launcher
import com.ursolgleb.controlparental.databinding.ActivityMainBinding


class Ulities: AccessibilityService() {

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