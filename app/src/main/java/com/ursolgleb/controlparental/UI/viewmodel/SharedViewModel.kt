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
import com.ursolgleb.controlparental.ControlParentalApp
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.BlockedEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.HttpURLConnection
import java.net.URL

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val blockedAppsProcessing = mutableSetOf<String>()
    private val mutex_updateBDApps = Mutex()
    private val mutex_addApp = Mutex()

    private val _todosApps = MutableStateFlow<List<AppEntity>>(emptyList())
    val todosApps: StateFlow<List<AppEntity>> = _todosApps

    private val _blockedApps = MutableStateFlow<List<BlockedEntity>>(emptyList())
    val blockedApps: StateFlow<List<BlockedEntity>> = _blockedApps

    private val _appsNoBloqueados = MutableStateFlow<List<BlockedEntity>>(emptyList())
    val appsNoBloqueados: StateFlow<List<BlockedEntity>> = _appsNoBloqueados

    private val _filteredApps = MutableStateFlow<List<AppEntity>>(emptyList())
    val filteredApps: StateFlow<List<AppEntity>> = _filteredApps

    init {
        // 🔥 Cargar datos en tiempo real cuando se cree el ViewModel
        Log.w("SharedViewModel", "init SharedViewModel $_blockedApps")
        loadAppsFromDatabaseASharedViewModer()
    }

    private fun loadAppsFromDatabaseASharedViewModer() {
        // 🔹 Lanzamos una primera corrutina para obtener la lista de aplicaciones instaladas
        viewModelScope.launch {
            // 🔥 Observamos la base de datos en tiempo real con Flow
            ControlParentalApp.dbApps.appDao().getAllApps()
                .stateIn(viewModelScope) // Se suscribe solo una vez
                .collect { apps ->
                    val sortedApps = apps
                        .map { app ->
                            app to getHorasDeUso(app.packageName, 1)
                        }
                        .sortedByDescending { (_, horasDeUso) -> horasDeUso } // Ordenamos por horas de uso
                        .map { (app, _) -> app } // Extraemos solo la lista de apps ordenada
                    _todosApps.value = sortedApps
                }
        }

        // 🔹 Lanzamos una segunda corrutina para obtener la lista de aplicaciones bloqueadas
        viewModelScope.launch {
            ControlParentalApp.dbApps.blockedDao().getAllBlockedApps()
                .stateIn(viewModelScope)
                .collect { blockedApps ->
                    val sortedBlockedApps = blockedApps
                        .map { app ->
                            app to getHorasDeUso(app.packageName, 1)
                        }
                        .sortedByDescending { (_, horasDeUso) -> horasDeUso } // Ordenamos por horas de uso
                        .map { (app, _) -> app } // Extraemos solo la lista de apps ordenada
                    _blockedApps.value = sortedBlockedApps
                }
        }
    }

    // 🔥 ✅ Para agregar una app a la base de datos ⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰
    fun addAppBlockListBD(packageName: String) {
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
    }

    // 🔥 ✅ Función para actualizar la base de datos de aplicaciones ⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰⏰ MUTEX
    suspend fun updateBDApps() {
        val locked = mutex_updateBDApps.tryLock() // Intenta obtener el bloqueo
        if (!locked) {
            Log.w("SharedViewModelMUTEX", "updateBDApps ya está en ejecución")
            return
        }
        try {
            Log.e("SharedViewModel", "555 999 updateBDApps")
            val appsNuevas = getNuevasAppsEnSistema()
            if (appsNuevas.isNotEmpty()) {
                addAppsAAppsBD(appsNuevas)
                addAppsABlockedBD(appsNuevas)
            }
        } catch (e: DeadObjectException) {
            Log.e("SharedViewModel", "DeadObjectException: ${e.message}")
        } catch (e: Exception) {
            Log.e("SharedViewModel", "Error en updateBDApps: ${e.message}")
        } finally {
            if (locked) { // Solo desbloquea si realmente ha adquirido el bloqueo
                mutex_updateBDApps.unlock()
            }
        }
    }

    // 🔥 ✅ Función para agregar apps a la base de datos ♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️♻️
    suspend fun addAppsAAppsBD(appsNuevas: List<ApplicationInfo>) {
        val pm = getApplication<Application>().packageManager
        if (appsNuevas.isEmpty()) return
        val appDao = ControlParentalApp.dbApps.appDao()
        for (app in appsNuevas) {
            //👁️🧠var contentRating = getAppAgeRatingScraper(app.packageName)
            val newApp = AppEntity(
                packageName = app.packageName,
                appName = app.loadLabel(pm).toString(),
                appIcon = app.loadIcon(pm).toString(),
                appCategory = app.category.toString(),
                contentRating = "?",
                appIsSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            )
            appDao.insertApp(newApp)
            Log.d(
                "MainAdminActivity",
                "Nueva App insertada a AppsBD: ${newApp.appName}, " +
                        "Package: ${newApp.packageName}, " +
                        "Rating: ${newApp.contentRating}, " +
                        "System App: ${newApp.appIsSystemApp}, " +
                        "Category: ${newApp.appCategory}, " +
                        "Icon: ${newApp.appIcon}"
            )
        }
    }

    // 🔥 ✅ Función para agregar apps bloqueadas a la base de datos 👌👌👌👌👌👌👌👌👌👌👌
    fun addAppsABlockedBD(appsNuevas: List<ApplicationInfo>) {
        if (appsNuevas.isEmpty()) return
        for (app in appsNuevas) {
            addAppBlockListBD(app.packageName)
        }
    }


    suspend fun getAppsFromDB(): List<AppEntity> {
        return ControlParentalApp.dbApps.appDao().getAllApps().first()
    }

    suspend fun getBlockedAppsFromDB(): List<BlockedEntity> {
        return ControlParentalApp.dbApps.blockedDao().getAllBlockedApps().first()
    }

    suspend fun getNuevasAppsEnSistema(): List<ApplicationInfo> {
        val appsDeSistema = getAllAppsWithUIdeSistema()
        if (appsDeSistema.isEmpty()) return emptyList()
        val appsDeBD = getAppsFromDB()
        val paquetesEnBD = appsDeBD.map { it.packageName }.toSet()
        return appsDeSistema.filter { it.packageName !in paquetesEnBD }
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

    fun getHorasDeUso(packageName: String, dias: Int): Double {
        val usageStats = getUsageStats(dias)
        for (usageStat in usageStats) {
            if (usageStat.packageName == packageName) {
                val horasDeUso = usageStat.totalTimeInForeground / 3600000.0
                return horasDeUso
            }
        }
        return 0.0
    }

    fun getUsageStats(dias: Int): List<UsageStats> {
        val usageStatsManager =
            getApplication<Application>().getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyList() // Retorna una lista vacía si el servicio no está disponible
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60 * 24 * dias // Últimas X dias
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        ).orEmpty()
    }

    suspend fun getAppAgeRatingScraper(packageName: String): String {
        //val url = "https://play.google.com/store/apps/details?id=$packageName&hl=en"
        val url = "https://play.google.com/store/apps/details?id=$packageName"
        return withContext(Dispatchers.IO) {
            if (!isUrlExists(url)) {
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

    fun isUrlExists(url: String): Boolean {
        return try {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 3000 // Reducimos timeout para mejorar rapidez
                readTimeout = 3000
                instanceFollowRedirects = true // Permite seguir redirecciones
                setRequestProperty(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
                ) // User-Agent real
            }
            connection.responseCode in 200..399 // Acepta respuestas 2xx y redirecciones 3xx
        } catch (e: Exception) {
            false // Si hay error, asumimos que la URL no existe
        }
    }


    ////////////////////
    fun sort_todosAppsByUsage(days: Int) {
        viewModelScope.launch {
            val allApps = _todosApps.value
            val sortedList = allApps
                .map { app ->
                    app to getHorasDeUso(app.packageName, days) // Calculamos solo una vez
                }
                //.filter { (_, horasDeUso) -> horasDeUso > 0 } // Filtramos solo los que tienen uso
                .sortedByDescending { (_, horasDeUso) -> horasDeUso } // Ordenamos por horas de uso
                .map { (app, _) -> app } // Extraemos solo la lista de apps ordenada

            _todosApps.value = sortedList // Actualiza el flujo con la lista ordenada
            Log.w("filteredList", "filterAndSortAppsByUsage: $sortedList")
        }
    }

    fun sort_blockedAppsByUsage(days: Int) {
        viewModelScope.launch {
            val allApps = _blockedApps.value
            val sortedList = allApps
                .map { app ->
                    app to getHorasDeUso(app.packageName, days) // Calculamos solo una vez
                }
                //.filter { (_, horasDeUso) -> horasDeUso > 0 } // Filtramos solo los que tienen uso
                .sortedByDescending { (_, horasDeUso) -> horasDeUso } // Ordenamos por horas de uso
                .map { (app, _) -> app } // Extraemos solo la lista de apps ordenada

            _blockedApps.value = sortedList // Actualiza el flujo con la lista ordenada
            Log.w("filteredList", "filterAndSortAppsByUsage: $sortedList")
        }
    }




}

