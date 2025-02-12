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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ursolgleb.controlparental.ControlParentalApp
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.BlockedEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.HttpURLConnection
import java.net.URL

class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val _blockedApps = MutableLiveData<List<BlockedEntity>>(emptyList())
    val blockedApps: LiveData<List<BlockedEntity>> = _blockedApps
    private val _todosApps = MutableLiveData<List<AppEntity>>(emptyList())
    val todosApps: LiveData<List<AppEntity>> = _todosApps
    private val _appsNoBloqueados = MutableLiveData<List<AppEntity>>(emptyList())
    val appsNoBloqueados: LiveData<List<AppEntity>> = _appsNoBloqueados

    init {
        // 🔥 Cargar datos en tiempo real cuando se cree el ViewModel
        loadAppsFromDatabase()
    }

    private fun loadAppsFromDatabase() {
        viewModelScope.launch {
            ControlParentalApp.dbApps.appDao().getAllApps().collect { apps ->
                _todosApps.value = apps
            }
        }
        viewModelScope.launch {
            ControlParentalApp.dbApps.blockedDao().getAllBlockedApps().collect { blockedApps ->
                _blockedApps.value = blockedApps
            }
        }
    }



    // 🔥 ✅ Para agregar una app a la base de datos
    fun addAppBlockList(
        packageName: String,
        onSuccess: ((String) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val blockedDao = ControlParentalApp.dbApps.blockedDao()
            val appDao = ControlParentalApp.dbApps.appDao()
            val existingBlockedApp = blockedDao.getBlockedAppByPackageName(packageName)
            if (existingBlockedApp == null) {
                val newBlockedApp = BlockedEntity(packageName = packageName)
                blockedDao.insertBlockedApp(newBlockedApp)
                withContext(Dispatchers.Main) {
                    onSuccess?.invoke("Nueva App insertada a BLOCKED bd: $packageName") // ✅ Callback si la operación fue exitosa
                }
            } else {
                val app = appDao.getApp(packageName)
                withContext(Dispatchers.Main) {
                    onError?.invoke("${app?.appName ?: "App"} ya está bloqueada") // ✅ Callback si ya está bloqueada
                }
            }
        }
    }

    suspend fun updateBDApps(pm: PackageManager) {
        try {
            val appsNuevas = getNuevasAppsEnSistema(pm)
            if (appsNuevas.isEmpty()) return
            agregarAppsAAppsBD(appsNuevas, pm)
            agregarAppsABlockedBD(appsNuevas)
        } catch (e: DeadObjectException) {
            Log.e("SharedViewModel", "DeadObjectException: ${e.message}")
        } catch (e: Exception) {
            Log.e("SharedViewModel", "Error en updateBDApps: ${e.message}")
        }
    }


    // 🔥 ✅ Función para agregar apps a la base de datos y LiveData
    suspend fun agregarAppsAAppsBD(appsNuevas: List<ApplicationInfo>, pm: PackageManager) {
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

    // 🔥 ✅ Función para agregar apps bloqueadas a la base de datos
    suspend fun agregarAppsABlockedBD(appsNuevas: List<ApplicationInfo>) {
        if (appsNuevas.isEmpty()) return
        val blockedDao = ControlParentalApp.dbApps.blockedDao()
        for (app in appsNuevas) {
            val newBlockedApp = BlockedEntity(packageName = app.packageName)
            blockedDao.insertBlockedApp(newBlockedApp)
        }
    }


    suspend fun getAppsFromDB(): List<AppEntity> {
        return ControlParentalApp.dbApps.appDao().getAllApps().first()
    }

    private suspend fun getNuevasAppsEnSistema(pm: PackageManager): List<ApplicationInfo> {
        val appsDeSistema = getAllAppsWithUIdeSistema(pm)
        if (appsDeSistema.isEmpty()) return emptyList()
        val appsDeBD = getAppsFromDB()
        val paquetesEnBD = appsDeBD.map { it.packageName }.toSet()
        return appsDeSistema.filter { it.packageName !in paquetesEnBD }
    }

    fun getAllAppsWithUIdeSistema(pm: PackageManager): List<ApplicationInfo> {
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
            getApplication<Application>().getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 * 60 * 24 * dias // Últimas X dias
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )
    }

    suspend fun getAppAgeRatingScraper(packageName: String): String {
        //val url = "https://play.google.com/store/apps/details?id=$packageName&hl=en"
        val url = "https://play.google.com/store/apps/details?id=$packageName"
        return withContext(Dispatchers.IO) {
            if (!urlExists(url)) {
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
                    Log.w("MainAdminActivity", "No se encontró el elemento de clasificación")
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

    fun urlExists(url: String): Boolean {
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

}

