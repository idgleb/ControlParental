package com.ursolgleb.controlparental

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.DeadObjectException
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.collection.emptyLongList
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
    private val mutexUpdateTiempoDeUso = Mutex()
    private val mutexGlobal = Mutex()

    val todosAppsFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    val blockedAppsFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    val todosAppsMenosBloqueadosFlow = MutableStateFlow<List<AppEntity>>(emptyList())

    val mutexUpdateBDAppsStateFlow = MutableStateFlow(false)
    val mostrarBottomSheetActualizadaFlow = MutableStateFlow(false)

    // mutexGlobal
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

    // mutexGlobal Actualizar la base de datos de aplicaciones
    fun updateBDApps() {

        if (!isInicieDeLecturaTermina) {
            inicieDelecturaDeBD()
            return
        }

        coroutineScope.launch {
            val locked = mutexUpdateBDApps.tryLock()
            if (!locked) {
                mutexUpdateBDAppsStateFlow.value = mutexUpdateBDApps.isLocked
                Log.w("AppDataRepository1", "updateBDApps ya está en ejecución")
                return@launch
            }

            mutexUpdateBDAppsStateFlow.value = true
            try {
                mutexGlobal.withLock {
                    Log.e("AppDataRepository1", "Ejecutando updateBDApps")
                    Log.w("AppDataRepository1", "Start getNuevasAppsEnSistema...")
                    val appsNuevas = getNuevasAppsEnSistema(context)
                    Log.w("AppDataRepository1", "Finish getNuevasAppsEnSistema.")
                    if (appsNuevas.isNotEmpty()) {
                        Log.w("AppDataRepository1", "Start addListaAppsBD...")
                        addListaAppsBD(appsNuevas)
                        Log.w("AppDataRepository1", "Finish addListaAppsBD.")
                    }
                }
            } catch (e: Exception) {
                Log.e("AppDataRepository1", "Error en updateBDApps: ${e.message}")
            } finally {
                mutexUpdateBDApps.unlock()
                mutexUpdateBDAppsStateFlow.value = false
                Log.e("AppDataRepository1", "updateBDApps finalizada")
            }
        }

    }

    // (no puede ser mutexGlobal) Agregar nuevas apps a la BD
    suspend fun addListaAppsBD(appsNuevas: List<ApplicationInfo>) {
        if (appsNuevas.isEmpty()) return
        val pm = context.packageManager

        Log.e("AppDataRepository1", "Start crear getTiempoDeUsoSeconds...")
        val tiempoDeUso = getTiempoDeUsoSeconds(appsNuevas) { app -> app.packageName }
        Log.e("AppDataRepository1", "Finish crear getTiempoDeUsoSeconds.")

        Log.d("AppDataRepository1", "Start crear nuevasEntidades...")
        val nuevasEntidades = appsNuevas.map { app ->

            // condicion a donde poner las nuevas apps(block, entretenimiento o siempre disponibles)
            val blocked = true
            var entretenimiento = app.category in listOf(
                ApplicationInfo.CATEGORY_GAME,
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO
            )
            if (blocked) entretenimiento = false
            //////

            AppEntity(
                packageName = app.packageName,
                appName = app.loadLabel(pm).toString(),
                appIcon = app.loadIcon(pm).toString(),
                appCategory = app.category.toString(),
                contentRating = "?",
                appIsSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                tiempoUsoSegundosHoy = tiempoDeUso[app.packageName]?.get(0) ?: 0,
                tiempoUsoSegundosSemana = tiempoDeUso[app.packageName]?.get(1) ?: 0,
                tiempoUsoSegundosMes = tiempoDeUso[app.packageName]?.get(2) ?: 0,
                blocked = blocked,
                usoLimitPorDiaMinutos = 0,
                entretenimiento = entretenimiento
            )
        }
        Log.d("AppDataRepository1", "Finish crear nuevasEntidades.")

        try {
            Log.w("AppDataRepository1", "Start agregamos insertListaApps nuevasEntidades a BD...")
            appDao.insertListaApps(nuevasEntidades)
            Log.w("AppDataRepository1", "Finish agregamos insertListaApps nuevasEntidades a BD.")
        } catch (e: Exception) {
            Log.e(
                "AppDataRepository",
                "addListaAppsBD Error al insertar apps en la BD: ${e.message}"
            )
        } finally {
            Log.d(
                "AppDataRepository",
                "addListaAppsBD Nueva Lista App insertada a AppsBD: ${appsNuevas.size}"
            )
        }
    }

    // (no puede ser mutexGlobal) Obtener nuevas apps instaladas en el sistema
    suspend fun getNuevasAppsEnSistema(context: Context): List<ApplicationInfo> {
        val installedApps = getAllAppsWithUIdeSistema(context)
        if (installedApps.isEmpty()) return emptyList()

        val appsDeBD = appDao.getAllApps().first()
        val paquetesEnBD = appsDeBD.map { it.packageName }.toSet()
        val nuevosApps = installedApps.filter { it.packageName !in paquetesEnBD }
        Log.e("AppDataRepository", "NUEVAS APPS: ${nuevosApps.joinToString { it.packageName }}")
        return nuevosApps

    }

    // mutexGlobal Bloquear apps en la BD
    fun addAppsASiempreBloqueadasBD(appsNuevas: List<AppEntity>) {
        if (appsNuevas.isEmpty()) return
        val appsBloqueadas = appsNuevas.map {
            it.copy(
                blocked = true,
                usoLimitPorDiaMinutos = 0,
                entretenimiento = false
            )
        }


        coroutineScope.launch {
            try {
                mutexGlobal.withLock {
                    Log.d(
                        "AppDataRepository",
                        "addAppsASiempreBloqueadasBD agregamos nuevasEntidades a BD..."
                    )
                    appDao.insertListaApps(appsBloqueadas)
                }
            } catch (e: Exception) {
                Log.e(
                    "AppDataRepository",
                    "addAppsASiempreBloqueadasBD Error al insertar apps bloqueadas en la BD: ${e.message}"
                )
            } finally {
                Log.d(
                    "AppDataRepository",
                    "addAppsASiempreBloqueadasBD Nueva Lista Apps bloqueadas: ${appsNuevas.size}"
                )
            }
        }


    }

    // mutexGlobal
    fun addAppsASiempreDisponiblesBD(appsNuevas: List<AppEntity>) {
        if (appsNuevas.isEmpty()) return
        val appsBloqueadas = appsNuevas.map {
            it.copy(
                blocked = false,
                usoLimitPorDiaMinutos = 0,
                entretenimiento = false
            )
        }


        coroutineScope.launch {
            try {
                mutexGlobal.withLock {
                    Log.d(
                        "AppDataRepository",
                        "addAppsASiempreDisponiblesBD agregamos nuevasEntidades a BD..."
                    )
                    appDao.insertListaApps(appsBloqueadas)
                }
            } catch (e: Exception) {
                Log.e(
                    "AppDataRepository",
                    "addAppsASiempreDisponiblesBD Error al insertar apps en la BD: ${e.message}"
                )
            } finally {
                Log.d(
                    "AppDataRepository",
                    "addAppsASiempreDisponiblesBD Nueva Lista Apps: ${appsNuevas.size}"
                )
            }
        }

    }

    // mutexGlobal
    fun addAppsAEntretenimientoBD(appsNuevas: List<AppEntity>) {
        if (appsNuevas.isEmpty()) return
        val appsBloqueadas = appsNuevas.map { it.copy(blocked = false, entretenimiento = true) }


        coroutineScope.launch {
            try {
                mutexGlobal.withLock {
                    Log.d(
                        "AppDataRepository",
                        "addAppsAEntretenimientoBD agregamos nuevasEntidades a BD..."
                    )
                    appDao.insertListaApps(appsBloqueadas)
                }
            } catch (e: Exception) {
                Log.e(
                    "AppDataRepository",
                    "addAppsAEntretenimientoBD Error al insertar apps en la BD: ${e.message}"
                )
            } finally {
                Log.d(
                    "AppDataRepository",
                    "addAppsAEntretenimientoBD Nueva Lista Apps: ${appsNuevas.size}"
                )
            }
        }


    }

    // mutexGlobal
    fun renovarTiempoUsoApp(pkgName: String) {
        coroutineScope.launch {
            try {
                mutexUpdateBDAppsStateFlow.value = true
                val tiempoDeUsoMap = mutableMapOf<String, MutableList<Long>>()
                val listPkgName = listOf(pkgName)
                val tiempoDeUso = getTiempoDeUsoSeconds(listPkgName) { app -> app }
                listPkgName.forEach { app ->
                    val tiempoHoy = tiempoDeUso[app]?.get(0) ?: 0
                    val tiempoSemana = tiempoDeUso[app]?.get(1) ?: 0
                    val tiempoMes = tiempoDeUso[app]?.get(2) ?: 0
                    tiempoDeUsoMap[app] = mutableListOf(tiempoHoy, tiempoSemana, tiempoMes)
                    Log.w("AppDataRepository1", "Renovar tiempo de uso de app: ${app}")
                }
                if (tiempoDeUsoMap.isNotEmpty()) appDao.updateUsageTimes(tiempoDeUsoMap)
            } catch (e: Exception) {
                Log.e("AppDataRepository1", "Error en renovarTiempoUsoApp: ${e.message}")
            } finally {
                mutexUpdateBDAppsStateFlow.value = false
                Log.e("AppDataRepository1", "renovarTiempoUsoApp finalizada")
            }
        }
    }

    // mutexGlobal
    fun updateTiempoUsoApps() = coroutineScope.launch {
        val locked = mutexUpdateTiempoDeUso.tryLock()
        if (!locked) {
            mutexUpdateBDAppsStateFlow.value = mutexUpdateTiempoDeUso.isLocked
            Log.w("AppDataRepository1", "UpdateTiempoDeUso ya está en ejecución")
            return@launch
        }

        mutexUpdateBDAppsStateFlow.value = true
        try {
            Log.w("AppDataRepository1", "Empezamos actualizar tiempo de uso de apps")

            val tiempoDeUsoMap = mutableMapOf<String, MutableList<Long>>()

            val apps = appDao.getAllApps().first()
            val tiempoDeUso = getTiempoDeUsoSeconds(apps) { app -> app.packageName }
            apps.forEach { app ->
                val tiempoHoy = tiempoDeUso[app.packageName]?.get(0) ?: 0
                val tiempoSemana = tiempoDeUso[app.packageName]?.get(1) ?: 0
                val tiempoMes = tiempoDeUso[app.packageName]?.get(2) ?: 0

                if (app.tiempoUsoSegundosHoy != tiempoHoy ||
                    app.tiempoUsoSegundosSemana != tiempoSemana ||
                    app.tiempoUsoSegundosMes != tiempoMes
                ) {
                    tiempoDeUsoMap[app.packageName] =
                        mutableListOf(tiempoHoy, tiempoSemana, tiempoMes)
                    Log.w("AppDataRepository1", "Actualizado tiempo de uso de app: ${app.appName}")
                }
            }
            if (tiempoDeUsoMap.isNotEmpty()) appDao.updateUsageTimes(tiempoDeUsoMap)

        } catch (e: Exception) {
            Log.e("AppDataRepository1", "Error en UpdateTiempoDeUso: ${e.message}")
        } finally {
            mutexUpdateTiempoDeUso.unlock()
            mutexUpdateBDAppsStateFlow.value = false
            Log.e("AppDataRepository1", "UpdateTiempoDeUso finalizada")
        }
    }

    fun addNuevoPkgBD(pkgName: String) {
        val listaApplicationInfo = listOfNotNull(getApplicationInfo(pkgName))
        coroutineScope.launch { addListaAppsBD(listaApplicationInfo) }
    }

    fun siEsNuevoPkg(packageName: String) =
        todosAppsFlow.value.none { it.packageName == packageName }


    /////////// otros funciones sin comunicar con BD

    // Obtener todas las apps con UI
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

    // Obtener el tiempo de uso
    /*    fun <T> getTiempoDeUsoSeconds(
            apps: List<T>,
            getPackageName: (T) -> String
        ): Map<String, List<Long>> {

            var duracion = 0L

            val startTimes = getStartTimes()

            val endTime = System.currentTimeMillis()

            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                    ?: return emptyMap()

            val tiempoDeUso = mutableMapOf<String, MutableList<Long>>()

            duracion = System.currentTimeMillis() - endTime
            Log.d("AppDataRepository1", "Duración getStartTimes es  $duracion")

            apps.forEach { app ->
                val packageName = getPackageName(app)
                tiempoDeUso[packageName] = mutableListOf() // Inicializa la lista para cada app
                startTimes.take(3).forEach { startTime -> // Iteramos sobre los startTimes
                    val totalTimeInForegroundSinUltimaSesionAbierta =
                        usageStatsManager.queryAndAggregateUsageStats(
                            startTime,
                            endTime
                        )[packageName]?.totalTimeInForeground?.div(1000) ?: 0
                    tiempoDeUso[packageName]?.add(totalTimeInForegroundSinUltimaSesionAbierta) // Añade el tiempo
                }
            }

            duracion = System.currentTimeMillis() - endTime
            Log.d("AppDataRepository1", "Duración apps.forEach es  $duracion")

            // Obtén los eventos en ese rango de tiempo
            val startTimeDia = startTimes[3]
            val usageEvents = usageStatsManager.queryEvents(startTimeDia, endTime)
            val event = UsageEvents.Event()

            duracion = System.currentTimeMillis() - endTime
            Log.d("AppDataRepository1", "Duración queryEvents es  $duracion")

            //val lastForegroundTimestamp = 0L // Almacena el timestamp del último FOREGROUND
            val lastForegroundTimestampPorPkg = mutableMapOf<String, Long>()

            // Recorrer eventos
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (tiempoDeUso[event.packageName] == null) continue
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> lastForegroundTimestampPorPkg[event.packageName] =
                        event.timeStamp

                    UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED, UsageEvents.Event.DEVICE_SHUTDOWN ->
                        lastForegroundTimestampPorPkg.remove(event.packageName)
                }
            }

            duracion = System.currentTimeMillis() - endTime
            Log.d("AppDataRepository1", "Duración Recorrer eventos es  $duracion")

            // Si la app sigue en foreground, calcular tiempo adicional
            for ((packageName, lastForegroundTimestamp) in lastForegroundTimestampPorPkg) {
                val timeSessionAbierta = (endTime - lastForegroundTimestamp) / 1000
                tiempoDeUso[packageName]?.set(
                    0,
                    tiempoDeUso[packageName]?.get(0)?.plus(timeSessionAbierta) ?: 0
                )
            }

            duracion = System.currentTimeMillis() - endTime
            Log.d("AppDataRepository1", "Duración calcular tiempo adicional es  $duracion")

            // Calcular la duración total del metodo
            duracion = System.currentTimeMillis() - endTime
            Log.d("AppDataRepository1", "Duración total de getTiempoDeUsoSeconds es  $duracion")

            return tiempoDeUso
        }*/

    /*fun <T> getTiempoDeUsoSeconds2(
        apps: List<T>,
        getPackageName: (T) -> String
    ): Map<String, List<Long>> {

        val duracion: Long

        val startTimes = getStartTimes()

        val endTime = System.currentTimeMillis()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyMap()

        val tiempoDeUso = mutableMapOf<String, MutableList<Long>>()

        startTimes.take(3).forEach { startTime -> // Iteramos sobre los startTimes
            val usageStatsMap: Map<String, UsageStats> =
                usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
            apps.forEach { app ->
                val packageName = getPackageName(app)
                tiempoDeUso.getOrPut(packageName) { mutableListOf() }// Inicializa la lista si no existe
                val totalTimeInForegroundSinUltimaSesionAbierta =
                    usageStatsMap[packageName]?.totalTimeInForeground?.div(1000) ?: 0
                tiempoDeUso[packageName]?.add(totalTimeInForegroundSinUltimaSesionAbierta) // Añade el tiempo
            }
        }

        Log.d("AppDataRepository1", "tiempoDeUso $tiempoDeUso")

        // Obtén los eventos en ese rango de tiempo
        val startTimeDia = startTimes[3]
        val usageEvents = usageStatsManager.queryEvents(startTimeDia, endTime)
        val event = UsageEvents.Event()

        // Almacena el timestamp del último FOREGROUND
        val lastForegroundTimestampPorPkg = mutableMapOf<String, Long>()

        // Recorrer eventos
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            when (event.eventType) {
                UsageEvents.Event.DEVICE_SHUTDOWN, UsageEvents.Event.DEVICE_STARTUP ->
                    lastForegroundTimestampPorPkg.clear()
            }

            if (tiempoDeUso[event.packageName] == null) continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED ->
                    lastForegroundTimestampPorPkg[event.packageName] = event.timeStamp

                UsageEvents.Event.ACTIVITY_STOPPED ->
                    lastForegroundTimestampPorPkg.remove(event.packageName)
            }
        }

        // Si la app sigue en foreground, calcular tiempo adicional
        for ((packageName, lastForegroundTimestamp) in lastForegroundTimestampPorPkg) {
            val timeSessionAbierta = (endTime - lastForegroundTimestamp) / 1000
            tiempoDeUso[packageName]?.set(
                0,
                tiempoDeUso[packageName]?.get(0)?.plus(timeSessionAbierta) ?: 0
            )
        }

        // Calcular la duración total del metodo
        duracion = System.currentTimeMillis() - endTime
        Log.d("AppDataRepository1", "Duración total de getTiempoDeUsoSeconds es  $duracion")

        return tiempoDeUso
    }*/


    fun <T> getTiempoDeUsoSeconds(
        apps: List<T>,
        getPackageName: (T) -> String
    ): Map<String, List<Long>> {

        val duracion: Long

        val startTimes = getStartTimes()
        val endTime = System.currentTimeMillis()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyMap()


        // 🔹 1. Obtener tiempo total de uso registrado
        val tiempoDeUso = mutableMapOf<String, MutableList<Long>>()

        startTimes.take(3).forEach { startTime -> // Iteramos sobre los startTimes
            val usageStatsMap: Map<String, UsageStats> =
                usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
            apps.forEach { app ->
                val packageName = getPackageName(app)
                tiempoDeUso.getOrPut(packageName) { mutableListOf() }// Inicializa la lista si no existe
                val totalTimeInForegroundSinUltimaSesionAbierta =
                    usageStatsMap[packageName]?.totalTimeInForeground?.div(1000) ?: 0
                tiempoDeUso[packageName]?.add(totalTimeInForegroundSinUltimaSesionAbierta) // Añade el tiempo
            }
        }

/*        val packageNameSet = apps.map(getPackageName).toSet()
        val tiempoDeUso = mutableMapOf<String, MutableList<Long>>()
        startTimes.take(3).forEach { startTime ->
            Log.d("AppDataRepository1", "Start $startTime")
            val usageStatsMap = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)
            usageStatsMap?.forEach { (packageName, stats) ->
                if (packageName in packageNameSet) {
                    tiempoDeUso.getOrPut(packageName) { mutableListOf() }// Inicializa la lista si no existe
                    tiempoDeUso[packageName]?.add(stats.totalTimeInForeground / 1000)
                    Log.d("AppDataRepository1", "Tiempo de uso $packageName: ${stats.totalTimeInForeground / 1000}")
                }
            }
        }*/

        Log.d("AppDataRepository1", "tiempoDeUso $tiempoDeUso")

        // 🔹 2. Revisar si hay apps en uso actualmente
        val startTimeDia = startTimes[3]
        val usageEvents = usageStatsManager.queryEvents(startTimeDia, endTime)
        val event = UsageEvents.Event()
        val lastForegroundTimestampsPorPkg = mutableMapOf<String, Long>()
        while (usageEvents?.hasNextEvent() == true) {
            usageEvents.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.DEVICE_SHUTDOWN, UsageEvents.Event.DEVICE_STARTUP ->
                    lastForegroundTimestampsPorPkg.clear() // todos apps cerrados
            }
            if (tiempoDeUso[event.packageName] == null) continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED ->
                    lastForegroundTimestampsPorPkg[event.packageName] = event.timeStamp //en uso la app
                UsageEvents.Event.ACTIVITY_STOPPED ->
                    lastForegroundTimestampsPorPkg.remove(event.packageName) // app cerrada
            }
        } //y si en events falta ACTIVITY_STOPPED en algun app por tema de crash por ejemplo,
        // entonces este codico va a pensar que esta app aun sigue en uso actualmente?

        // 🔹 3. Sumar el tiempo de las apps que aún están abiertas
        lastForegroundTimestampsPorPkg.forEach { (packageName, lastTimestamp) ->
            Log.d("AppDataRepository1", "App en primer plano: $packageName con timestamp $lastTimestamp")
            val tiempoSesionAbierta = (endTime - lastTimestamp) / 1000
            tiempoDeUso[packageName]?.set(
                0,
                tiempoDeUso[packageName]?.get(0)?.plus(tiempoSesionAbierta) ?: 0
            )
        }

        Log.d("AppDataRepository1", "tiempoDeUso2 $tiempoDeUso")

        // Calcular la duración total del metodo
        duracion = System.currentTimeMillis() - endTime
        Log.d("AppDataRepository1", "Duración total de getTiempoDeUsoSeconds es  $duracion")

        return tiempoDeUso
    }


    private fun getStartTimes(): MutableList<Long> {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTimes: MutableList<Long> = mutableListOf()

        startTimes.add(calendar.timeInMillis) //Insertamos "Hoy"

        calendar.add(Calendar.DAY_OF_YEAR, -7) // Calculamos "Semana"

        startTimes.add(calendar.timeInMillis) //Insertamos "Semana"

        calendar.add(Calendar.DAY_OF_YEAR, -23) // Calculamos "Mes"

        startTimes.add(calendar.timeInMillis) //Insertamos "Mes"

        // Volvemos a sumar los dias restados  para calcular el dia anterior
        calendar.add(Calendar.DAY_OF_YEAR, 29)
        startTimes.add(calendar.timeInMillis)  // Insertamos "1 Día" al final

        return startTimes
    }

    // Obtener clasificación de edad de una app
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

    // Obtener el ícono de una aplicación
    fun getAppIcon(packageName: String): Drawable? {
        return try {
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

    fun getApplicationInfo(packageName: String): ApplicationInfo? {
        return try {
            val pm = context.packageManager
            pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            null // Si no se encuentra la app, retorna null
        }
    }

    fun clear() {
        coroutineScope.cancel() // ✅ Cancelar todas las corrutinas cuando ya no sea necesario
    }



}
