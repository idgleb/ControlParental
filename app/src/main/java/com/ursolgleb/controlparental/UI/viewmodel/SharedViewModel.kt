package com.ursolgleb.controlparental.UI.viewmodel

import android.app.Application
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.DeadObjectException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ursolgleb.controlparental.data.local.AppDatabase
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.BlockedEntity
import com.ursolgleb.controlparental.utils.Fun
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    application: Application,
    appDatabase: AppDatabase
) : AndroidViewModel(application) {

    private val appDao = appDatabase.appDao()

    private val mutex_updateBDApps = Mutex()
    private val _mutexUpdateBDAppsState = MutableStateFlow(false)
    val mutexUpdateBDAppsState: StateFlow<Boolean> = _mutexUpdateBDAppsState

    private val mutex_inicieDelecturaDeBD = Mutex()

    private val mutex_Global = Mutex() // Mutex compartido para sincronización

    private val _todosApps = MutableStateFlow<List<AppEntity>>(emptyList())
    val todosApps: StateFlow<List<AppEntity>> = _todosApps

    private val _blockedApps = MutableStateFlow<List<AppEntity>>(emptyList())
    val blockedApps: StateFlow<List<AppEntity>> = _blockedApps

    private val _todosAppsMenosBlaqueados = MutableStateFlow<List<AppEntity>>(emptyList())
    val todosAppsMenosBlaqueados: StateFlow<List<AppEntity>> = _todosAppsMenosBlaqueados

    var inicieDeLecturaDeBD = false

    init {
        Log.w("SharedViewModel3", "init SharedViewModel $inicieDeLecturaDeBD")
        // 🔥 Carga inicial de datos de la base de datos
        inicieDelecturaDeBD()
        // 🔥 Cargar datos en tiempo real cuando se cree el ViewModel
        loadAppsFromDatabaseASharedViewModel()
    }

    private fun inicieDelecturaDeBD() {
        val locked = mutex_inicieDelecturaDeBD.tryLock()
        if (!locked) {
            Log.w("SharedViewModelMUTEX", "inicieDelecturaDeBD ya está en ejecución")
            return
        }

        try {

            viewModelScope.launch(Dispatchers.IO) {
                mutex_Global.withLock { // Bloquea mientras ejecuta esta parte
                    inicieDeLecturaDeBD = true
                    Log.e("SharedViewModel3", "inicieDelecturaDeBD(): $inicieDeLecturaDeBD")

                    _todosApps.value = appDao.getAllApps().first()
                    _blockedApps.value = _todosApps.value.filter { it.blocked }
                    _todosAppsMenosBlaqueados.value = _todosApps.value.filter { !it.blocked }

                    Log.e("SharedViewModel", "APPS DE BD 111: ${_todosApps.value}")
                } // Se libera el mutex cuando termina
            }


        } catch (e: DeadObjectException) {
            Log.e("SharedViewModel", "DeadObjectException: ${e.message}")
        } catch (e: Exception) {
            Log.e("SharedViewModel", "Error en inicieDeLecturaDeBD: ${e.message}")
        } finally {
            if (locked) { // Solo desbloquea si realmente ha adquirido el bloqueo
                mutex_inicieDelecturaDeBD.unlock()
            }
        }

    }

    private fun loadAppsFromDatabaseASharedViewModel() {

        viewModelScope.launch(Dispatchers.IO) {
            appDao.getAllApps().collect { apps ->
                _todosApps.value = apps
                _blockedApps.value = apps.filter { it.blocked }
                _todosAppsMenosBlaqueados.value = apps.filter { !it.blocked }
            }

        }

    }


    // 🔥 ✅ Función para actualizar la base de datos de aplicaciones ⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰ MUTEX
    fun updateBDApps() {

        viewModelScope.launch(Dispatchers.IO) {
            if (!inicieDeLecturaDeBD) {
                inicieDelecturaDeBD()
            }

            val locked = mutex_updateBDApps.tryLock() // Intenta obtener el bloqueo
            if (!locked) {
                // Ya estaba bloqueado; actualizamos el stateFlow
                _mutexUpdateBDAppsState.value = mutex_updateBDApps.isLocked
                Log.w("SharedViewModelMUTEX", "updateBDApps ya está en ejecución")
                return@launch
            }
            // Actualizamos el stateFlow al adquirir el lock
            _mutexUpdateBDAppsState.value = true
            try {
                mutex_Global.withLock { // ponemos en la cola de ejecución
                    Log.e("SharedViewModel", "333 updateBDApps")
                    val appsNuevas = getNuevasAppsEnSistema()
                    if (appsNuevas.isNotEmpty()) {
                        addListaAppsBD(appsNuevas)
                    }
                }

            } catch (e: DeadObjectException) {
                Log.e("SharedViewModel", "DeadObjectException: ${e.message}")
            } catch (e: Exception) {
                Log.e("SharedViewModel", "Error en updateBDApps: ${e.message}")
            } finally {
                if (locked) { // Solo desbloquea si realmente ha adquirido el bloqueo
                    // Liberamos el lock y actualizamos el stateFlow
                    mutex_updateBDApps.unlock()
                    _mutexUpdateBDAppsState.value = false
                }
            }
        }
    }


    // 🔥 ✅ Función para agregar apps a la base de datos ♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️
    suspend fun addListaAppsBD(appsNuevas: List<ApplicationInfo>) {
        if (appsNuevas.isEmpty()) return
        val pm = getApplication<Application>().packageManager
        appDao.insertListaApps(appsNuevas.map { app ->
            //👁️🧠var contentRating = getAppAgeRatingScraper(app.packageName)
            val entretenimiento = when (app.category) {
                ApplicationInfo.CATEGORY_GAME,
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO -> true

                else -> false
            }

            AppEntity( //formamos lista de entidades para BD
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
        })
        Log.d("MainAdminActivity", "Nueva Lista App insertada a AppsBD: $appsNuevas")
    }

    // 🔥 ✅ Función para blaquear apps en la base de datos ️♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️
    suspend fun addAppsABlockedBD(appsNuevas: List<AppEntity>) {
        if (appsNuevas.isEmpty()) return
        for (app in appsNuevas) {
            app.blocked = true
        }
        appDao.insertListaApps(appsNuevas)
        Log.d("MainAdminActivity", "Nueva Lista Apps bloqueada: $appsNuevas")
    }


    fun getNuevasAppsEnSistema(): List<ApplicationInfo> {
        val appsDeSistema = getAllAppsWithUIdeSistema()
        if (appsDeSistema.isEmpty()) return emptyList()

        val appsDeBD = _todosApps.value
        Log.e("APPS DE BD", "APPS DE BD 222: $appsDeBD")
        val paquetesEnBD = appsDeBD.map { it.packageName }.toSet()

        val nuevosApps = appsDeSistema.filter { it.packageName !in paquetesEnBD }

        // 🔹 Formateamos la lista para que sea más legible en los logs
        val listaFormateada = nuevosApps.joinToString(separator = "\n") { app ->
            "Nombre: ${app.loadLabel(getApplication<Application>().packageManager)}, " +
                    "Package: ${app.packageName}"
        }

        Log.e("SharedViewModel", "NUEVOS APPS:\n$listaFormateada")

        return nuevosApps
    }

    fun getAllAppsWithUIdeSistema(): List<ApplicationInfo> {
        val pm = getApplication<Application>().packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val appsWithUI = mutableListOf<ApplicationInfo>()
        for (app in installedApps) {
            // Obtener todas las actividades de la app
            val intent = Intent(Intent.ACTION_MAIN, null)
            intent.setPackage(app.packageName) // Filtrar solo las actividades de esta app
            val activities = pm.queryIntentActivities(intent, 0)
            if (activities.isNotEmpty()) {
                appsWithUI.add(app) // La app tiene al menos una actividad
            }
        }
        return appsWithUI
    }

    suspend fun getAppIcon(packageName: String, context: Context): Drawable? {
        return withContext(Dispatchers.IO) {
            try {
                val packageManager = context.packageManager
                packageManager.getApplicationIcon(packageName)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getTiempoDeUsoSeconds(packageName: String, dias: Int): Long {
        val usageStats = getUsageStats(dias)
        for (usageStat in usageStats) {
            if (usageStat.packageName == packageName) {
                val segundosDeUso = usageStat.totalTimeInForeground / 1000
                return segundosDeUso
            }
        }
        return 0
    }

    fun getUsageStats(dias: Int): List<UsageStats> {
        val usageStatsManager =
            getApplication<Application>().getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyList() // Retorna lista vacía si el servicio no está disponible

        val calendar = Calendar.getInstance().apply {
            add(
                Calendar.DAY_OF_YEAR,
                -dias
            ) // Restar la cantidad de días(0 es hoy, -1 Desde ayer a las 00:00:00  hasta ahora., etc)
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


    suspend fun getAppAgeRatingScraper(packageName: String): String {
        //val url = "https://play.google.com/store/apps/details?id=$packageName&hl=en"
        val url = "https://play.google.com/store/apps/details?id=$packageName"
        return withContext(Dispatchers.IO) {
            if (!Fun.isUrlExists(url)) {
                Log.w("MainAdminActivity", "La app no existe en Google Play")
                return@withContext ""
            }
            try {
                val doc: Document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36")
                    .timeout(5000) // Espera hasta 5s para evitar timeout
                    .get()
                val ageRatingElement = doc.select("span[itemprop=contentRating]").first()
                if (ageRatingElement == null) {
                    Log.w(
                        "MainAdminActivity",
                        "No se encontró el elemento de clasificación"
                    )
                    return@withContext ""
                }
                ageRatingElement.text()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("MainAdminActivity", "Error al obtener la clasificación", e)
                ""
            }
        }
    }





}

