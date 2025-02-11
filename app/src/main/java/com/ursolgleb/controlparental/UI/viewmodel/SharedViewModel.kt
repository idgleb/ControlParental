package com.ursolgleb.controlparental.UI.viewmodel

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.DeadObjectException
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.ursolgleb.controlparental.ControlParentalApp
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.BlockedEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.HttpURLConnection
import java.net.URL

class SharedViewModel : ViewModel() {

    private val _blockedApps = MutableLiveData<List<BlockedEntity>>(emptyList())
    val blockedApps: LiveData<List<BlockedEntity>> = _blockedApps
    private val _todosApps = MutableLiveData<List<AppEntity>>(emptyList())
    val todosApps: LiveData<List<AppEntity>> = _todosApps


    // 🔥 ✅ Función para actualizar toda la lista
    fun updateBlockedAppsInterfaz(newList: List<BlockedEntity>) {
        _blockedApps.value = newList // ✅ Solo maneja datos, no UI
    }

    // 🔥 ✅ Función para agregar una nueva app bloqueada
    fun addBlockedAppInterfaz(newBlockedApp: BlockedEntity) {
        val currentList = _blockedApps.value.orEmpty().toMutableList()
        currentList.add(newBlockedApp)
        _blockedApps.value = currentList
    }


    // 🔥 ✅ Nueva función para agregar una app a la base de datos y actualizar LiveData
    fun addAppBlockList(
        packageName: String,
        onSuccess: ((String) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val blockedDao = ControlParentalApp.dbApps.blockedDao()
            val appDao = ControlParentalApp.dbApps.appDao()
            val existingBlockedApp = blockedDao.getBlockedAppByPackageName(packageName)
            val app = appDao.getApp(packageName)
            if (existingBlockedApp == null) {
                val newBlockedApp = BlockedEntity(packageName = packageName)
                blockedDao.insertBlockedApp(newBlockedApp)
                withContext(Dispatchers.Main) {
                    addBlockedAppInterfaz(newBlockedApp)
                    onSuccess?.invoke("Nueva App insertada a BLOCKED bd: $packageName") // ✅ Callback si la operación fue exitosa
                }
            } else {
                withContext(Dispatchers.Main) {
                    onError?.invoke("${app?.appName ?: "App"} ya está bloqueada") // ✅ Callback si ya está bloqueada
                }
            }
        }
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

    suspend fun loadBlockedAppsDeBDaViewModel() {
        val blockedApps = ControlParentalApp.dbApps.blockedDao().getAllBlockedApps()
        withContext(Dispatchers.Main) {
            updateBlockedAppsInterfaz(blockedApps)
        }
    }

    // 🔥 ✅ Función para agregar apps a la base de datos y LiveData
    suspend fun agregarAppsAAppsBD(appsNuevas: List<ApplicationInfo>, pm: PackageManager) {
        if (appsNuevas.isEmpty()) return
        val appDao = ControlParentalApp.dbApps.appDao()
        val newAppsList = mutableListOf<AppEntity>()
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
            newAppsList.add(newApp)
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
        withContext(Dispatchers.Main) {
            _todosApps.value = _todosApps.value.orEmpty() + newAppsList
        }
    }

    // 🔥 ✅ Función para agregar apps bloqueadas a la base de datos y LiveData
    suspend fun agregarAppsABlockedBD(appsNuevas: List<ApplicationInfo>) {
        if (appsNuevas.isEmpty()) return
        val blockedDao = ControlParentalApp.dbApps.blockedDao()
        val newBlockedList = mutableListOf<BlockedEntity>()
        for (app in appsNuevas) {
            val newBlockedApp = BlockedEntity(packageName = app.packageName)
            blockedDao.insertBlockedApp(newBlockedApp)
            newBlockedList.add(newBlockedApp)
        }
        // 🔥 ✅ Actualizar LiveData con las nuevas apps bloqueadas
        withContext(Dispatchers.Main) {
            _blockedApps.value = _blockedApps.value.orEmpty() + newBlockedList
        }
    }

    private suspend fun getNuevasAppsEnSistema(pm: PackageManager): List<ApplicationInfo> {
        val appsDeSistema = getAllAppsWithUIdeSistema(pm)
        if (appsDeSistema.isEmpty()) return emptyList()
        val appsDeBD = getAppsFromDB()
        val paquetesEnBD = appsDeBD.map { it.packageName }.toSet()
        return appsDeSistema.filter { it.packageName !in paquetesEnBD }
    }

    suspend fun getAppsFromDB(): List<AppEntity> {
        return ControlParentalApp.dbApps.appDao().getAllApps().first()
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


}

