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

    private val appDao: AppDao = appDatabase.appDao()

    private val mutexInicieDelecturaDeBD = Mutex()

    private val mutexUpdateBDApps = Mutex()
    private val mutexGlobal = Mutex()

    val todosAppsFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    val blockedAppsFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    val todosAppsMenosBloqueadosFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    val mutexUpdateBDAppsState = MutableStateFlow(false)

    fun inicieDelecturaDeBD() {
        val locked = mutexInicieDelecturaDeBD.tryLock()
        if (!locked) {
            Log.w("ControlParentalApp", "inicieDelecturaDeBD ya está en ejecución")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                mutexGlobal.withLock {
                    Log.e("ControlParentalApp", "Iniciando lectura de la base de datos")

                    val apps = appDao.getAllApps().first()

                    // 🔥 Guardamos los datos en el repositorio compartido
                    todosAppsFlow.value = apps
                    blockedAppsFlow.value = apps.filter { it.blocked }
                    todosAppsMenosBloqueadosFlow.value =
                        apps.filter { !it.blocked }

                    Log.e("ControlParentalApp", "Apps cargadas en la base de datos: ${apps.size}")
                }
            } catch (e: DeadObjectException) {
                Log.e("ControlParentalApp", "DeadObjectException: ${e.message}")
            } catch (e: Exception) {
                Log.e("ControlParentalApp", "Error en inicieDelecturaDeBD: ${e.message}")
            } finally {
                if (locked) {
                    mutexInicieDelecturaDeBD.unlock()
                }
            }
        }
    }

    fun cargarAppsEnBackgroundDesdeBD() {
        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.appDao().getAllApps().collect { apps ->
                todosAppsFlow.value = apps
                blockedAppsFlow.value = apps.filter { it.blocked }
                todosAppsMenosBloqueadosFlow.value = apps.filter { !it.blocked }

                Log.d("ControlParentalApp", "Apps cargadas desde la base de datos: ${apps.size}")
            }
        }
    }


    // 🔥 ✅ Actualizar la base de datos de aplicaciones
    fun updateBDApps(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
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
                        addListaAppsBD(appsNuevas, context)
                    }
                }
            } catch (e: Exception) {
                Log.e("AppDataRepository", "Error en updateBDApps: ${e.message}")
            } finally {
                mutexUpdateBDApps.unlock()
                mutexUpdateBDAppsState.value = false
            }
        }

    }

    // 🔥 ✅ Agregar nuevas apps a la BD
    suspend fun addListaAppsBD(appsNuevas: List<ApplicationInfo>, context: Context) {
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
                tiempoUsoSegundosHoy = getTiempoDeUsoSeconds(context, app.packageName, 0),
                tiempoUsoSegundosSemana = getTiempoDeUsoSeconds(context, app.packageName, 7),
                tiempoUsoSegundosMes = getTiempoDeUsoSeconds(context, app.packageName, 30),
                blocked = true,
                usoLimitPorDiaMinutos = 0,
                entretenimiento = entretenimiento
            )
        }

        appDao.insertListaApps(nuevasEntidades)
        Log.d("AppDataRepository", "Nueva Lista App insertada a AppsBD: ${appsNuevas.size}")
    }

    // 🔥 ✅ Bloquear apps en la BD
    suspend fun addAppsABlockedBD(appsNuevas: List<AppEntity>) {
        if (appsNuevas.isEmpty()) return
        val appsBloqueadas = appsNuevas.map { it.copy(blocked = true) }
        appDao.insertListaApps(appsBloqueadas)
        Log.d("AppDataRepository", "Nueva Lista Apps bloqueadas: ${appsNuevas.size}")
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

        return installedApps.filter { app ->
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.setPackage(app.packageName)
            pm.queryIntentActivities(intent, 0).isNotEmpty()
        }
    }

    // 🔥 ✅ Obtener el tiempo de uso de una app
    fun getTiempoDeUsoSeconds(context: Context, packageName: String, dias: Int): Long {
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


}
