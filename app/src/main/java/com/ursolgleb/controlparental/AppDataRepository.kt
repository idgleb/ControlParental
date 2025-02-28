package com.ursolgleb.controlparental

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.DeadObjectException
import android.util.Log
import com.ursolgleb.controlparental.data.local.AppDatabase
import com.ursolgleb.controlparental.data.local.dao.AppDao
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.utils.Fun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDataRepository @Inject constructor(
    val appDatabase: AppDatabase,
    val context: Context
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val appDao: AppDao = appDatabase.appDao()

    private var isInicieDeLecturaTermina = false

    private val mutexInicieDelecturaDeBD = Mutex()
    private val mutexUpdateBDApps = Mutex()
    private val mutexGlobal = Mutex()

    val todosAppsFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    val blockedAppsFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    val todosAppsMenosBloqueadosFlow = MutableStateFlow<List<AppEntity>>(emptyList())

    val mutexUpdateBDAppsState = MutableStateFlow(false)
    val mostrarBottomSheetActualizadaFlow = MutableStateFlow(false)

    fun inicieDelecturaDeBD() {
        val locked = mutexInicieDelecturaDeBD.tryLock()
        if (!locked) {
            Log.w("AppDataRepository", "inicieDelecturaDeBD ya está en ejecución")
            return
        }

        coroutineScope.launch {
            try {
                mutexGlobal.withLock {
                    Log.e("AppDataRepository", "Iniciando inicieDelecturaDeBD")
                    val apps = appDao.getAllApps().first()
                    // 🔥 Guardamos los datos en el repositorio compartido
                    todosAppsFlow.value = apps
                    blockedAppsFlow.value = apps.filter { it.blocked }
                    todosAppsMenosBloqueadosFlow.value =
                        apps.filter { !it.blocked }
                    Log.e(
                        "AppDataRepository",
                        "Apps cargadas de BD en inicieDelecturaDeBD: ${apps.size}"
                    )
                }
            } catch (e: DeadObjectException) {
                Log.e("AppDataRepository", "DeadObjectException: ${e.message}")
            } catch (e: Exception) {
                Log.e("AppDataRepository", "Error en inicieDelecturaDeBD: ${e.message}")
            } finally {
                cargarAppsEnBackgroundDesdeBD()
                isInicieDeLecturaTermina = true
                Log.e("AppDataRepository", "inicieDelecturaDeBD finalizada")
                if (locked) mutexInicieDelecturaDeBD.unlock()
                updateBDApps()
            }
        }
    }

    private fun cargarAppsEnBackgroundDesdeBD() {
        coroutineScope.launch {
            appDatabase.appDao().getAllApps().collect { apps ->
                todosAppsFlow.value = apps
                val blockedApps = apps.filter { it.blocked }

                if (blockedApps.toList() != blockedAppsFlow.value.toList()) { // ✅ Compara contenido, no referencias
                    blockedAppsFlow.value =
                        blockedApps.toList() // ✅ Nueva instancia, garantiza emisión
                    mostrarBottomSheetActualizadaFlow.value = true
                }

                todosAppsMenosBloqueadosFlow.value = apps.filter { !it.blocked }

                Log.d(
                    "AppDataRepository",
                    "Apps cargadas de BD en cargarAppsEnBackgroundDesdeBD: ${apps.size}"
                )
            }
        }
    }

    // 🔥 ✅ Actualizar la base de datos de aplicaciones
    fun updateBDApps() {

        if (!isInicieDeLecturaTermina) {
            inicieDelecturaDeBD()
            return
        }

        coroutineScope.launch {
            val locked = mutexUpdateBDApps.tryLock()
            if (!locked) {
                mutexUpdateBDAppsState.value = mutexUpdateBDApps.isLocked
                Log.w("AppDataRepository", "updateBDApps ya está en ejecución")
                return@launch
            }

            mutexUpdateBDAppsState.value = true
            try {
                mutexGlobal.withLock {
                    Log.e("AppDataRepository", "Ejecutando updateBDApps")
                    val appsNuevas = getNuevasAppsEnSistema(context)
                    if (appsNuevas.isNotEmpty()) {
                        addListaAppsBD(appsNuevas)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppDataRepository", "Error en updateBDApps: ${e.message}")
            } finally {
                mutexUpdateBDApps.unlock()
                mutexUpdateBDAppsState.value = false
                Log.e("AppDataRepository", "updateBDApps finalizada")
            }
        }

    }

    // 🔥 ✅ Agregar nuevas apps a la BD
    suspend fun addListaAppsBD(appsNuevas: List<ApplicationInfo>) {
        if (appsNuevas.isEmpty()) return
        val pm = context.packageManager

        val nuevasEntidades = appsNuevas.map { app ->

            val entretenimiento = app.category in listOf(
                ApplicationInfo.CATEGORY_GAME,
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO
            )
            AppEntity(
                packageName = app.packageName,
                appName = app.loadLabel(pm).toString(),
                appIcon = app.loadIcon(pm).toString(),
                appCategory = app.category.toString(),
                contentRating = "?",
                appIsSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                tiempoUsoSegundosHoy = getTiempoDeUsoSeconds(app.packageName, 0),
                tiempoUsoSegundosSemana = getTiempoDeUsoSeconds(app.packageName, 7),
                tiempoUsoSegundosMes = getTiempoDeUsoSeconds(app.packageName, 30),
                blocked = true,
                usoLimitPorDiaMinutos = 0,
                entretenimiento = entretenimiento
            )


        }

        appDao.insertListaApps(nuevasEntidades)
        Log.d("AppDataRepository", "Nueva Lista App insertada a AppsBD: ${appsNuevas.size}")
    }

    // 🔥 ✅ Bloquear apps en la BD
    suspend fun addAppsASiempreBloqueadasBD(appsNuevas: List<AppEntity>) {
        if (appsNuevas.isEmpty()) return
        val appsBloqueadas = appsNuevas.map {
            it.copy(
                blocked = true,
                usoLimitPorDiaMinutos = 0,
                entretenimiento = false
            )
        }
        Log.d("AppDataRepository", "Nueva Lista Apps bloqueadas: ${appsNuevas.size}")

        try {
            withContext(Dispatchers.IO) { // ✅ Mover la operación a un contexto seguro
                appDao.insertListaApps(appsBloqueadas)
            }
            Log.d("AppDataRepository", "Nueva Lista 77 Apps bloqueadas: ${appsNuevas.size}")
        } catch (e: Exception) {
            Log.e("AppDataRepository", "Error al insertar apps bloqueadas en la BD: ${e.message}")
        }
    }

    suspend fun addAppsASiempreDisponiblesBD(appsNuevas: List<AppEntity>) {
        if (appsNuevas.isEmpty()) return
        val appsBloqueadas = appsNuevas.map {
            it.copy(
                blocked = false,
                usoLimitPorDiaMinutos = 0,
                entretenimiento = false
            )
        }
        appDao.insertListaApps(appsBloqueadas)
        Log.d("AppDataRepository", "Nueva Lista Apps desponibles: ${appsNuevas.size}")
    }

    suspend fun addAppsAEntretenimientoBD(appsNuevas: List<AppEntity>) {
        if (appsNuevas.isEmpty()) return
        val appsBloqueadas = appsNuevas.map { it.copy(blocked = false, entretenimiento = true) }
        appDao.insertListaApps(appsBloqueadas)
        Log.d("AppDataRepository", "Nueva Lista Apps Entretenimiento: ${appsNuevas.size}")
    }

    fun addNuevoPkgBD(pkgName: String) {
        val listaApplicationInfo = listOfNotNull(getApplicationInfo(pkgName))
        coroutineScope.launch {
            addListaAppsBD(listaApplicationInfo)
        }
    }

    // 🔥 ✅ Obtener nuevas apps instaladas en el sistema
    fun getNuevasAppsEnSistema(context: Context): List<ApplicationInfo> {
        val installedApps = getAllAppsWithUIdeSistema(context)
        if (installedApps.isEmpty()) return emptyList()

        val appsDeBD = todosAppsFlow.value
        val paquetesEnBD = appsDeBD.map { it.packageName }.toSet()

        val nuevosApps = installedApps.filter { it.packageName !in paquetesEnBD }

        Log.e("AppDataRepository", "NUEVAS APPS: ${nuevosApps.joinToString { it.packageName }}")
        return nuevosApps
    }

    // 🔥 ✅ Obtener todas las apps con UI
    fun getAllAppsWithUIdeSistema(context: Context): List<ApplicationInfo> {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        return installedApps.filter { app -> siTieneUI(context, app.packageName) }
    }

    fun siTieneUI(context: Context, packageName: String): Boolean {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.setPackage(packageName)
        return pm.queryIntentActivities(intent, 0).isNotEmpty()
    }

    fun siEsNuevoPkg(packageName: String): Boolean =
        todosAppsFlow.value.none { it.packageName == packageName }


    // 🔥 ✅ Obtener el tiempo de uso de una app
    fun getTiempoDeUsoSeconds(packageName: String, dias: Int): Long {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return 0

        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -dias)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val usageStats = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
        return usageStats[packageName]?.totalTimeInForeground?.div(1000) ?: 0
    }

    // 🔥 ✅ Obtener clasificación de edad de una app
    suspend fun getAppAgeRatingScraper(packageName: String): String {
        val url = "https://play.google.com/store/apps/details?id=$packageName"
        return withContext(Dispatchers.IO) {
            if (!Fun.isUrlExists(url)) {
                Log.w("AppDataRepository", "La app no existe en Google Play")
                return@withContext ""
            }
            try {
                val doc: Document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                    .timeout(5000)
                    .get()
                doc.select("span[itemprop=contentRating]").first()?.text() ?: ""
            } catch (e: Exception) {
                Log.e("AppDataRepository", "Error al obtener la clasificación", e)
                ""
            }
        }
    }

    // 🔥 ✅ Obtener el ícono de una aplicación
    suspend fun getAppIcon(packageName: String, context: Context): Drawable? {
        return withContext(Dispatchers.IO) {
            try {
                val packageManager = context.packageManager
                packageManager.getApplicationIcon(packageName)
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("AppDataRepository", "Ícono no encontrado para $packageName")
                null
            } catch (e: Exception) {
                Log.e("AppDataRepository", "Error al obtener ícono de $packageName: ${e.message}")
                null
            }
        }
    }

    fun getApplicationInfo(packageName: String): ApplicationInfo? {
        return try {
            val pm = context.packageManager
            pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null // Si no se encuentra la app, retorna null
        }
    }


    // 🔥 ✅ Obtener estadísticas de uso de apps
    fun getUsageStats(context: Context, dias: Int): List<UsageStats> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyList() // Retorna lista vacía si el servicio no está disponible

        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -dias) // 0 = hoy, -1 = ayer hasta ahora, etc.
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startTime = calendar.timeInMillis // 00:00:00 del día correspondiente
        val endTime = System.currentTimeMillis() // Ahora mismo

        val aggregatedStats: Map<String, UsageStats> =
            usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)

        return aggregatedStats.values.toList()
    }

    fun clear() {
        coroutineScope.cancel() // ✅ Cancelar todas las corrutinas cuando ya no sea necesario
    }

    fun renovarTiempoUsoApp(pkgName: String) {
        val app = todosAppsFlow.value.find { it.packageName == pkgName }
        if (app != null) {
            coroutineScope.launch {
                appDao.updateApp(
                    app.copy(
                        tiempoUsoSegundosHoy = getTiempoDeUsoSeconds(pkgName, 0),
                        tiempoUsoSegundosSemana = getTiempoDeUsoSeconds(pkgName, 7),
                        tiempoUsoSegundosMes = getTiempoDeUsoSeconds(pkgName, 30)
                    )
                )
            }
            Log.w("AppDataRepository", "Renovar tiempo de uso de app: $pkgName")
        }
    }

    suspend fun updateTiempoUsoApps() = withContext(Dispatchers.IO) {
        val listaAppsCambiados = mutableListOf<AppEntity>()

        todosAppsFlow.value.forEach { app ->
            val tiempoHoy = getTiempoDeUsoSeconds(app.packageName, 0)
            val tiempoSemana = getTiempoDeUsoSeconds(app.packageName, 7)
            val tiempoMes = getTiempoDeUsoSeconds(app.packageName, 30)

            if (app.tiempoUsoSegundosHoy != tiempoHoy ||
                app.tiempoUsoSegundosSemana != tiempoSemana ||
                app.tiempoUsoSegundosMes != tiempoMes) {

                listaAppsCambiados.add(
                    app.copy(
                        tiempoUsoSegundosHoy = tiempoHoy,
                        tiempoUsoSegundosSemana = tiempoSemana,
                        tiempoUsoSegundosMes = tiempoMes
                    )
                )
            }
        }

        if (listaAppsCambiados.isNotEmpty()) {
            appDao.insertListaApps(listaAppsCambiados)
        }
    }





}
