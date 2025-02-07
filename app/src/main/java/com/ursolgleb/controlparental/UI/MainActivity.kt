package com.ursolgleb.controlparental.UI
//datos de uso
//accesibilidad

//acceptar encima
//aplicacion de administracion(solo bloqueo la pantalla)
//notificaciones
//batarea solo para app
//activar ubicacion
//permiso de ubicacion siempre
//permiso a tu actividad fisica

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ursolgleb.controlparental.databinding.ActivityMainBinding
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import android.widget.TextView
import android.widget.Toast
import com.ursolgleb.controlparental.ControlParentalApp
import com.ursolgleb.controlparental.services.AppBlockerService
import com.ursolgleb.controlparental.utils.Archivo
import com.ursolgleb.controlparental.utils.FileWatcher
import com.ursolgleb.controlparental.utils.Launcher
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.UpdateAppsBloquedasReceiver
import com.ursolgleb.controlparental.allowedApps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    lateinit var bindMain: ActivityMainBinding

    companion object {
        const val fileName = "log.txt"
    }

    private lateinit var fileWatcher: FileWatcher

    private lateinit var updateAppsBloquedasReceiver: UpdateAppsBloquedasReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bindMain = ActivityMainBinding.inflate(layoutInflater)
        setContentView(bindMain.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initUI()
        initFileWatcher()
        initListeners()
        initUpdateAppsBloquedasReceiver()
    }

    override fun onResume() {
        super.onResume()
        fileWatcher.startWatching()
        registrarRecivirUpdateApssBloqueadas()
        Log.w("MainActivityListaApps", "registerReceiver de onResume")
        actualizarListaAppsBloqueadas()
        Log.d("MainActivityListaApps", "Actualizando lista de apps bloqueadas de onResume")
    }

    override fun onPause() {
        super.onPause()
        fileWatcher.stopWatching()
        unregisterReceiver(updateAppsBloquedasReceiver)
        Log.w("MainActivityListaApps", "unregisterReceiver de onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        fileWatcher.stopWatching()
        unregisterReceiver(updateAppsBloquedasReceiver)
    }

    private fun initUpdateAppsBloquedasReceiver() {
        updateAppsBloquedasReceiver = UpdateAppsBloquedasReceiver { msg ->
            actualizarListaAppsBloqueadas()
            Log.d("MainActivityListaApps", "actualizarListaAppsBloqueadas de UpdateAppsBloquedasReceiver $msg")
        }
    }

    private fun registrarRecivirUpdateApssBloqueadas() {
        val filterUpdBlockApps = IntentFilter("com.ursolgleb.controlparental.UPDATE_BLOCKED_APPS")
        registerReceiver(updateAppsBloquedasReceiver, filterUpdBlockApps, RECEIVER_EXPORTED)
    }

    private fun actualizarListaAppsBloqueadas() {
        CoroutineScope(Dispatchers.IO).launch {
            val blockedApps = ControlParentalApp.db.blockedAppDao().getBlockedApps()
            val blockedAppsText = blockedApps.joinToString("\n") { app ->
                "🔒 ${app.packageName}  ⌚${app.blockedAt}"
            }
            withContext(Dispatchers.Main) {
                bindMain.tvAppsBloqueadas.text = blockedAppsText
            }
        }
    }

    private fun initFileWatcher() {
        fileWatcher = FileWatcher.observeFileChanges(this, fileName) { newContent ->
            CoroutineScope(Dispatchers.Main).launch {
                leerYMostrarInfoDeArchivo(fileName)
            }
        }
    }

    private fun initListeners() {

        bindMain.requestUsageStatsPermissionBoton.setOnClickListener {

            val intent = Intent("com.ursolgleb.controlparental.UPDATE_BLOCKED_APPS")
            intent.putExtra("mensaje", "¡Hola desde la boton!")
            sendBroadcast(intent)
            Log.w("MainActivityListaApps", "Broadcast enviado desde boton")

            if (!hasUsageStatsPermission(this)) {
                requestUsageStatsPermission(this)
            } else {
                val msg = "Ya tienes el permiso de Usage Stats"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                Archivo.appendTextToFile(this, fileName, "\n $msg")
            }
        }

        bindMain.requestAccessibilityServiceBoton.setOnClickListener {
            if (!isAccessibilityServiceEnabled(this, AppBlockerService::class.java)) {
                requestAccessibilityService()
            } else {
                val msg = "Ya tienes el servicio de accesibilidad habilitado"
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                Archivo.appendTextToFile(this, fileName, "\n $msg")
            }
        }

        bindMain.obtenerListaAppBoton.setOnClickListener {
            val pm = packageManager
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            bindMain.tvInformacion.appendAndScroll("Lista de Apps:")
            for (app in apps) {
                val msg = "App: ${app.loadLabel(pm)} - Package: ${app.packageName}"
                Log.d("AppBlockerService", msg)
                Archivo.appendTextToFile(this, fileName, "\n $msg")
            }
        }


        bindMain.getUsageStatsBoton.setOnClickListener {
            if (!hasUsageStatsPermission(this)) {
                requestUsageStatsPermission(this)
                return@setOnClickListener
            }
            val listUsageStatus: List<UsageStats> = getUsageStats()
            val msg = "Lista de Usage Stats: "
            Log.d("AppBlockerService", msg)
            Archivo.appendTextToFile(this, fileName, "\n $msg")

            val pm = packageManager  // Obtener PackageManager
            val listaUsageStatsTimeMasCero: MutableList<UsageStats> =
                listUsageStatus.filter { it.totalTimeInForeground > 0 } as MutableList<UsageStats>

            for (usageStats in listaUsageStatsTimeMasCero) {
                val appName = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(usageStats.packageName, 0))
                        .toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    "Desconocida"
                }

                val msg =
                    "App: $appName - Package: ${usageStats.packageName} - Usage: ${usageStats.totalTimeInForeground}"
                Log.d("AppBlockerService", msg)
                Archivo.appendTextToFile(this, fileName, "\n $msg")
            }
        }


        bindMain.getForegroundAppBoton.setOnClickListener {
            val foregroundApp = getForegroundApp()
            val msg = "App en primer plano: $foregroundApp"
            Log.d("AppBlockerService", msg)
            Archivo.appendTextToFile(this, fileName, "\n $msg")
        }

        bindMain.redirigirAlaPantallaDeInicioBoton.setOnClickListener {
            redirigirAlaPantallaDeInicio()
        }

        bindMain.mostrarPantallaDeBloqueoBoton.setOnClickListener {
            // Lógica para mostrar la pantalla de bloqueo
            mostrarPantallaDeBloqueo()
        }


        bindMain.mostrarLauncherBoton.setOnClickListener {
            val msg = "Launcher: ${Launcher.getDefaultLauncherPackageName(this)}"
            Log.w("AppBlockerService", msg)
            Archivo.appendTextToFile(this, fileName, "\n $msg")
        }

        bindMain.getSecureSettingsBoton.setOnClickListener {
            val msg = "SecureSettings: ${getSecureSettings(this)}"
            Log.e("AppBlockerService", msg)
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            Archivo.appendTextToFile(this, fileName, "\n $msg")
        }

        bindMain.showAllowedAppsBoton.setOnClickListener {
            allowedApps.showApps(this)
        }

        bindMain.clearFileBoton.setOnClickListener {
            Archivo.clearFile(this, fileName)
            bindMain.tvInformacion.text = ""
        }

        bindMain.actualizarBoton.setOnClickListener {
            leerYMostrarInfoDeArchivo(fileName)
        }
        bindMain.arribaBoton.setOnClickListener {
            bindMain.scrollView.scrollTo(0, 0)
        }

    }

    private fun leerYMostrarInfoDeArchivo(fileName: String) {
        val logContent = Archivo.readTextFromFile(this, fileName)
        bindMain.tvInformacion.text = logContent
        bindMain.tvInformacion.scrollToBottom()
    }

    private fun requestAccessibilityService() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        val msg = "Habilita el servicio de accesibilidad"
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        Archivo.appendTextToFile(this, fileName, "\n $msg")
    }

    fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+
            isAccessibilityServiceEnabledUsingSettings(context, serviceClass)
        } else {
            // Android 12 o inferior
            isAccessibilityServiceEnabledUsingManager(context, serviceClass)
        }
    }

    fun isAccessibilityServiceEnabledUsingManager(
        context: Context,
        serviceClass: Class<*>
    ): Boolean {
        val accessibilityManager =
            context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = accessibilityManager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.name == serviceClass.name) {
                return true
            }
        }
        return false
    }


    fun isAccessibilityServiceEnabledUsingSettings(
        context: Context,
        serviceClass: Class<*>
    ): Boolean {
        val expectedComponentName = "${context.packageName}/${serviceClass.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        ) == 1
    }

    private fun mostrarPantallaDeBloqueo() {
        val overlayIntent = Intent(this, LockScreenActivity::class.java)
        overlayIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(overlayIntent)
    }

    class LockScreenActivity {

    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageStatsPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_USAGE_ACCESS_SETTINGS,
            Uri.parse("package:$packageName")
        )
        context.startActivity(intent)
    }

    fun getUsageStats(): List<UsageStats> {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60 * 24  // Últimas 24 horas
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )
    }

    fun getForegroundApp(): String? {
        val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.runningAppProcesses?.firstOrNull()?.processName
    }

    fun redirigirAlaPantallaDeInicio() {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
    }


    fun getSecureSettings(context: Context): Map<String, String?> {
        val secureSettings = mutableMapOf<String, String?>()

        // Identificador único del dispositivo
        secureSettings["android_id"] = try {
            Settings.Secure.getString(context.contentResolver, "android_id")
        } catch (e: Exception) {
            null
        }

        // Proveedores de ubicación habilitados
        secureSettings["location_providers_allowed"] = try {
            Settings.Secure.getString(context.contentResolver, "location_providers_allowed")
        } catch (e: Exception) {
            null
        }

        // Indica si se permiten ubicaciones simuladas
        secureSettings["mock_location"] = try {
            Settings.Secure.getString(context.contentResolver, "mock_location")
        } catch (e: Exception) {
            null
        }

        // Indica si la configuración del usuario está completa
        secureSettings["user_setup_complete"] = try {
            Settings.Secure.getString(context.contentResolver, "user_setup_complete")
        } catch (e: Exception) {
            null
        }

        // Porcentaje de batería mostrado
        secureSettings["battery_percentage"] = try {
            Settings.Secure.getString(context.contentResolver, "battery_percentage")
        } catch (e: Exception) {
            null
        }

        // Modo No Molestar
        secureSettings["zen_mode"] = try {
            Settings.Secure.getString(context.contentResolver, "zen_mode")
        } catch (e: Exception) {
            null
        }

        // Sincronización habilitada
        secureSettings["sync_enabled"] = try {
            Settings.Secure.getString(context.contentResolver, "sync_enabled")
        } catch (e: Exception) {
            null
        }

        // Estado de la ubicación
        secureSettings["location_mode"] = try {
            Settings.Secure.getString(context.contentResolver, "location_mode")
        } catch (e: Exception) {
            null
        }

        // Radios habilitados en modo avión
        secureSettings["airplane_mode_radios"] = try {
            Settings.Secure.getString(context.contentResolver, "airplane_mode_radios")
        } catch (e: Exception) {
            null
        }

        // Proveedor de ubicación predeterminado
        secureSettings["default_location_provider"] = try {
            Settings.Secure.getString(context.contentResolver, "default_location_provider")
        } catch (e: Exception) {
            null
        }

        // Si el uso compartido de conexión está habilitado
        secureSettings["tethering_on"] = try {
            Settings.Secure.getString(context.contentResolver, "tethering_on")
        } catch (e: Exception) {
            null
        }

        // Habilita almacenamiento masivo USB
        secureSettings["usb_mass_storage"] = try {
            Settings.Secure.getString(context.contentResolver, "usb_mass_storage")
        } catch (e: Exception) {
            null
        }

        // Instalación de aplicaciones de fuentes desconocidas
        secureSettings["install_non_market_apps"] = try {
            Settings.Secure.getString(context.contentResolver, "install_non_market_apps")
        } catch (e: Exception) {
            null
        }

        // Preferencia de red
        secureSettings["network_preference"] = try {
            Settings.Secure.getString(context.contentResolver, "network_preference")
        } catch (e: Exception) {
            null
        }

        // Estado de Wi-Fi
        secureSettings["wifi_on"] = try {
            Settings.Secure.getString(context.contentResolver, "wifi_on")
        } catch (e: Exception) {
            null
        }

        // Estado de Bluetooth
        secureSettings["bluetooth_on"] = try {
            Settings.Secure.getString(context.contentResolver, "bluetooth_on")
        } catch (e: Exception) {
            null
        }

        // Estado del ahorro de batería
        secureSettings["battery_saver"] = try {
            Settings.Secure.getString(context.contentResolver, "battery_saver")
        } catch (e: Exception) {
            null
        }

        return secureSettings
    }

    fun TextView.scrollToBottom() {
        this.post {
            val scrollAmount =
                bindMain.tvInformacion.layout.getLineTop(bindMain.tvInformacion.lineCount) - bindMain.tvInformacion.height
            if (scrollAmount > 0) {
                bindMain.tvInformacion.scrollTo(0, scrollAmount)
            } else {
                bindMain.tvInformacion.scrollTo(0, 0)
            }
        }
    }

    fun TextView.appendAndScroll(text: String) {
        this.append(text)
        this.scrollToBottom()
    }


    private fun initUI() {
        bindMain.tvInformacion.movementMethod = android.text.method.ScrollingMovementMethod()
        leerYMostrarInfoDeArchivo(fileName)
        actualizarListaAppsBloqueadas()
        Log.d("MainActivityListaApps", "Actualizando lista de apps bloqueadas de initUI")
    }


}