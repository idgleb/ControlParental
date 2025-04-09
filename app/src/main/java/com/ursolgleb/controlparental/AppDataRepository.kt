package com.ursolgleb.controlparental

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.DeadObjectException
import android.util.Log
import com.ursolgleb.controlparental.data.local.AppDatabase
import com.ursolgleb.controlparental.data.local.dao.AppDao
import com.ursolgleb.controlparental.data.local.dao.HorarioDao
import com.ursolgleb.controlparental.data.local.dao.UsageEventDao
import com.ursolgleb.controlparental.data.local.dao.UsageStatsDao
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.HorarioEntity
import com.ursolgleb.controlparental.data.local.entities.UsageEventEntity
import com.ursolgleb.controlparental.data.local.entities.UsageStatsEntity
import com.ursolgleb.controlparental.utils.AppsFun
import com.ursolgleb.controlparental.utils.Fun
import com.ursolgleb.controlparental.utils.StatusApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDataRepository @Inject constructor(
    val appDatabase: AppDatabase,
    val context: Context
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val coroutineScopeHorario = CoroutineScope(Dispatchers.IO)

    private val appDao: AppDao = appDatabase.appDao()
    private val horarioDao: HorarioDao = appDatabase.horarioDao()
    private val usageEventDao: UsageEventDao = appDatabase.usageEventDao()
    private val usageStatsDao: UsageStatsDao = appDatabase.usageStatsDao()

    private var isInicieDeLecturaTermina = false

    private val mutexInicieDelecturaDeBD = Mutex()
    private val mutexUpdateBDApps = Mutex()
    private val mutexUpdateTiempoDeUso = Mutex()
    private val mutexGlobal = Mutex()

    val todosAppsFlow = MutableStateFlow<List<AppEntity>>(emptyList())

    val blockedAppsFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    val horarioAppsFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    val disponAppsFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    //
    val horariosFlow = MutableStateFlow<List<HorarioEntity>>(emptyList())

    val todosAppsMenosBloqueadosFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    var todosAppsMenosHorarioFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    val todosAppsMenosDisponFlow = MutableStateFlow<List<AppEntity>>(emptyList())

    val mutexUpdateBDAppsStateFlow = MutableStateFlow(false)
    val mostrarBottomSheetActualizadaFlow = MutableStateFlow(false)

    //========= Apps ===================================
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
                    // Guardamos los datos en el repositorio compartido
                    todosAppsFlow.value = apps
                    blockedAppsFlow.value = apps.filter { it.appStatus == StatusApp.BLOQUEADA.desc }
                    horarioAppsFlow.value = apps.filter { it.appStatus == StatusApp.HORARIO.desc }
                    disponAppsFlow.value = apps.filter { it.appStatus == StatusApp.DISPONIBLE.desc }
                    todosAppsMenosBloqueadosFlow.value =
                        apps.filter { it.appStatus != StatusApp.BLOQUEADA.desc }
                    todosAppsMenosHorarioFlow.value =
                        apps.filter { it.appStatus != StatusApp.HORARIO.desc }
                    todosAppsMenosDisponFlow.value =
                        apps.filter { it.appStatus != StatusApp.DISPONIBLE.desc }

                    horariosFlow.value = horarioDao.getAllHorarios().first()

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
            appDao.getAllApps().collect { apps ->
                todosAppsFlow.value = apps
                blockedAppsFlow.value = apps.filter { it.appStatus == StatusApp.BLOQUEADA.desc }
                horarioAppsFlow.value = apps.filter { it.appStatus == StatusApp.HORARIO.desc }
                disponAppsFlow.value = apps.filter { it.appStatus == StatusApp.DISPONIBLE.desc }

                todosAppsMenosBloqueadosFlow.value =
                    apps.filter { it.appStatus != StatusApp.BLOQUEADA.desc }
                todosAppsMenosHorarioFlow.value =
                    apps.filter { it.appStatus != StatusApp.HORARIO.desc }
                todosAppsMenosDisponFlow.value =
                    apps.filter { it.appStatus != StatusApp.DISPONIBLE.desc }

                Log.d(
                    "AppDataRepository",
                    "Apps cargadas de BD en cargarAppsEnBackgroundDesdeBD: ${apps.size}"
                )
            }

            horarioDao.getAllHorarios().collect { horarios ->
                horariosFlow.value = horarios
                Log.d("AppDataRepository", "Horarios cargados de BD: ${horarios.size}")
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
        val tiempoDeUso = getTiempoDeUsoHoy(appsNuevas) { app -> app.packageName }
        Log.e("AppDataRepository1", "Finish crear getTiempoDeUsoSeconds.")

        Log.d("AppDataRepository1", "Start crear nuevasEntidades...")

        val nuevasEntidades = appsNuevas.map { app ->

            // Obtener el ícono de la aplicación como Drawable y convertirlo a Bitmap
            val drawable = app.loadIcon(pm)
            val bitmap = AppsFun.drawableToBitmap(drawable)// 🔹 Convertir Drawable a Bitmap

            // condicion a donde poner las nuevas apps(block, horario o disponibles)
            val entretenimiento = app.category in listOf(
                ApplicationInfo.CATEGORY_GAME,
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO
            )
            var status = if (entretenimiento) StatusApp.HORARIO.desc else StatusApp.BLOQUEADA.desc
            status = StatusApp.BLOQUEADA.desc
            //////

            val timestampActual = System.currentTimeMillis()

            AppEntity(
                packageName = app.packageName,
                appName = app.loadLabel(pm).toString(),
                appIcon = bitmap,
                appCategory = app.category.toString(),
                contentRating = "?",
                isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                usageTimeToday = tiempoDeUso[app.packageName] ?: 0L,
                timeStempUsageTimeToday = timestampActual,
                appStatus = status,
                dailyUsageLimitMinutes = 0
            )
        }
        Log.d("AppDataRepository1", "Finish crear nuevasEntidades.")

        try {
            Log.w("AppDataRepository1", "Start agregamos insertListaApps nuevasEntidades a BD...")
            appDao.insertListaApps(nuevasEntidades)
            mostrarBottomSheetActualizadaFlow.value = true
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

    // (no puede ser mutexGlobal) Obtener nuevas apps instaladas en el sistema 🎈🎈🎈🎈🎈🎈🎈
    suspend fun getNuevasAppsEnSistema(context: Context): List<ApplicationInfo> {
        val installedApps = AppsFun.getAllAppsWithUIdeSistema(context)
        if (installedApps.isEmpty()) return emptyList()

        val appsDeBD = appDao.getAllApps().first()
        val paquetesEnBD = appsDeBD.map { it.packageName }.toSet()
        val nuevosApps = installedApps.filter { it.packageName !in paquetesEnBD }
        Log.e("AppDataRepository", "NUEVAS APPS: ${nuevosApps.joinToString { it.packageName }}")
        return nuevosApps

    }

    // mutexGlobal
    fun addAppsASiempreBloqueadasBD(appsNuevas: List<AppEntity>) {
        if (appsNuevas.isEmpty()) return

        val appsBloqueadas = appsNuevas.mapNotNull { app ->
            if (app.appStatus != StatusApp.BLOQUEADA.desc) {
                app.copy(
                    appStatus = StatusApp.BLOQUEADA.desc,
                    dailyUsageLimitMinutes = 0
                )
            } else null  // Si el estado ya es BLOQUEADA, omitimos este elemento
        }

        if (appsBloqueadas.isEmpty()) return

        coroutineScope.launch {
            try {
                mutexGlobal.withLock {
                    Log.d(
                        "AppDataRepository",
                        "addAppsASiempreBloqueadasBD agregamos apps a blocked a BD..."
                    )
                    appDao.insertListaApps(appsBloqueadas)
                    mostrarBottomSheetActualizadaFlow.value = true
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

        val appsDispon = appsNuevas.mapNotNull { app ->
            if (app.appStatus != StatusApp.DISPONIBLE.desc) {
                app.copy(
                    appStatus = StatusApp.DISPONIBLE.desc,
                    dailyUsageLimitMinutes = 0,
                )
            } else null
        }

        if (appsDispon.isEmpty()) return

        coroutineScope.launch {
            try {
                mutexGlobal.withLock {
                    Log.d(
                        "AppDataRepository",
                        "addAppsASiempreDisponiblesBD agregamos apps a disponibles a BD..."
                    )
                    appDao.insertListaApps(appsDispon)
                    mostrarBottomSheetActualizadaFlow.value = true
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
    fun addAppsAHorarioBD(appsNuevas: List<AppEntity>) {
        if (appsNuevas.isEmpty()) return

        val appsHorario = appsNuevas.mapNotNull { app ->
            if (app.appStatus != StatusApp.HORARIO.desc) {
                app.copy(appStatus = StatusApp.HORARIO.desc)
            } else null
        }

        if (appsHorario.isEmpty()) return

        coroutineScope.launch {
            try {
                mutexGlobal.withLock {
                    Log.d(
                        "AppDataRepository",
                        "addAppsAEntretenimientoBD agregamos apps a Entretenimiento a BD..."
                    )
                    appDao.insertListaApps(appsHorario)
                    mostrarBottomSheetActualizadaFlow.value = true
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

    fun addNuevoPkgBD(pkgName: String) {
        val listaApplicationInfo = listOfNotNull(AppsFun.getApplicationInfo(context, pkgName))
        coroutineScope.launch { addListaAppsBD(listaApplicationInfo) }
    }

    fun siEsNuevoPkg(packageName: String) = todosAppsFlow.value.none { it.packageName == packageName } //🎈🎈🎈🎈🎈🎈
    //===================================================

    //========= Horarios ===============================

    fun addHorarioBD(horario: HorarioEntity) {
        coroutineScopeHorario.launch {
            try {
                horarioDao.insertHorario(horario)
            }catch (e: Exception){
                Log.e("AppDataRepository", "Error al insertar horario en la BD: ${e.message}")
            }
        }
    }


    //=================================================


    //==============TiempoUso===========HOY
    // mutexGlobal
    fun renovarTiempoUsoAppHoy(pkgName: String) {
        coroutineScope.launch {
            try {
                mutexUpdateBDAppsStateFlow.value = true
                val tiempoDeUsoMap = mutableMapOf<String, Long>()
                val listPkgName = listOf(pkgName)
                val tiempoDeUso = getTiempoDeUsoHoy(listPkgName) { app -> app }
                listPkgName.forEach { app ->
                    val tiempoHoy = tiempoDeUso[app]
                    tiempoDeUsoMap[app] = tiempoHoy ?: 0L
                    Log.w("AppDataRepository1", "Renovar tiempo de uso de app hoy: ${app}")
                }
                if (tiempoDeUsoMap.isNotEmpty()) appDao.updateUsageTimeHoy(tiempoDeUsoMap)
            } catch (e: Exception) {
                Log.e("AppDataRepository1", "Error en renovarTiempoUsoApp: ${e.message}")
            } finally {
                mutexUpdateBDAppsStateFlow.value = false
                Log.e("AppDataRepository1", "renovarTiempoUsoApp finalizada")
            }
        }
    }

    // mutexGlobal
    fun updateTiempoUsoAppsHoy() = coroutineScope.launch {
        val locked = mutexUpdateTiempoDeUso.tryLock()
        if (!locked) {
            mutexUpdateBDAppsStateFlow.value = mutexUpdateTiempoDeUso.isLocked
            Log.w("AppDataRepository1", "updateTiempoUsoAppsHoy ya está en ejecución")
            return@launch
        }

        mutexUpdateBDAppsStateFlow.value = true
        try {
            Log.w("AppDataRepository1", "Empezamos actualizar tiempo de uso de apps hoy")

            val tiempoDeUsoMapHoy = mutableMapOf<String, Long>()

            val appsBD = appDao.getAllApps().first()
            val tiempoDeUsoHoy = getTiempoDeUsoHoy(appsBD) { app -> app.packageName }
            appsBD.forEach { appBD ->
                val tiempoDeUsoHoyApp = tiempoDeUsoHoy[appBD.packageName] ?: 0L
                if (appBD.usageTimeToday != tiempoDeUsoHoyApp) {
                    tiempoDeUsoMapHoy[appBD.packageName] = tiempoDeUsoHoyApp ?: 0L
                    Log.w(
                        "AppDataRepository1",
                        "♻️ Actualizado tiempo de uso de app hoy: ${appBD.appName}"
                    )
                }
            }
            if (tiempoDeUsoMapHoy.isNotEmpty()) appDao.updateUsageTimeHoy(tiempoDeUsoMapHoy)

        } catch (e: Exception) {
            Log.e("AppDataRepository1", "Error en updateTiempoUsoAppsHoy: ${e.message}")
        } finally {
            mutexUpdateTiempoDeUso.unlock()
            mutexUpdateBDAppsStateFlow.value = false
            Log.e("AppDataRepository1", "updateTiempoUsoAppsHoy finalizada")
        }
    }

    // 🔹 Obtener el tiempo de uso de hoy incluso el tiempo de apps abiertas ♻️♻️♻️🎈solo para lista Apps
    suspend fun <T> getTiempoDeUsoHoy(
        apps: List<T>,
        getPackageName: (T) -> String
    ): Map<String, Long> {

        val startTimeHoy = Fun.getTimeAtras(0) // "Hoy a las 00:00:00"
        val endTimeAhora = System.currentTimeMillis() // tiempo actual

        Log.d("getStatsHoy", "getStatsHoy ${Fun.dateFormat.format(startTimeHoy)}")

        val listaApps = apps.map { getPackageName(it) }
        val statsMapBD = mutableMapOf<String, Long>()

        // 🔹 Obtener eventos de la base de datos solo para lista de apps
        val usageEventsBD = getEventsFromDatabase(startTimeHoy, endTimeAhora, listaApps)

        // 🔹 Variables para manejar múltiples apps en primer plano
        val activeApps = mutableMapOf<String, Long>() // packageName -> timestamp de inicio

        // Mapa para almacenar los últimos dos eventos de cada app(para calcular el uso de la app que tienen conciquencia rara de los eventos)
        val lastTwoEventsByApp = mutableMapOf<String, MutableList<Int>>()

        // 🔹 Procesar eventos en orden cronológico
        val sortedEvents = usageEventsBD.sortedBy { it.timestamp }
        sortedEvents.forEach { event ->

            // formamos 2 ultimos eventos por app
            val eventsForApp = lastTwoEventsByApp.getOrPut(event.packageName) { mutableListOf() }
            // Agrega el evento actual
            eventsForApp.add(event.eventType)
            // Limita a tres eventos
            if (eventsForApp.size > 3) eventsForApp.removeAt(0) // Remueve el evento más antiguo si hay más de tres

            val eventTime = event.timestamp

            when (event.eventType) {
                //1
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // Registrar el inicio de la sesión de esta app
                    if (activeApps[event.packageName] == null) {
                        activeApps[event.packageName] = eventTime
                    }
                }
                //23
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    // si primero evento ACTIVITY_STOPPED es significa que la app estaba abierta, por eso sumamos desde el inicio hasta eventTime
                    if (eventsForApp.size == 1) {
                        val usageDuration = eventTime - startTimeHoy
                        statsMapBD.getOrPut(event.packageName) { 0L }
                        statsMapBD[event.packageName] =
                            (statsMapBD[event.packageName] ?: 0L) + usageDuration
                    }
                    // si ultimos dos eventos es ACTIVITY_PAUSED,ACTIVITY_RESUMED es incorrecto por eso no cerrar la sesion
                    if (!(lastTwoEventsByApp[event.packageName]?.get(0) == UsageEvents.Event.ACTIVITY_PAUSED &&
                                lastTwoEventsByApp[event.packageName]?.get(1) == UsageEvents.Event.ACTIVITY_RESUMED)
                    ) {
                        // Cerrar la sesión de esta app si estaba activa
                        activeApps[event.packageName]?.let { startTime ->
                            val usageDuration = eventTime - startTime
                            statsMapBD.getOrPut(event.packageName) { 0L }
                            statsMapBD[event.packageName] =
                                (statsMapBD[event.packageName] ?: 0L) + usageDuration
                            activeApps.remove(event.packageName) // Eliminar de la lista de activas
                        }
                    }
                }
                // 26, 27
                UsageEvents.Event.DEVICE_SHUTDOWN, UsageEvents.Event.DEVICE_STARTUP -> {
                    // Cerrar las sesiónes de TODAS las apps activas
                    activeApps.forEach { (app, startTime) ->
                        val usageDuration = eventTime - startTime
                        statsMapBD.getOrPut(app) { 0L }
                        statsMapBD[app] = (statsMapBD[app] ?: 0L) + usageDuration
                    }
                    activeApps.clear() // Limpiar todas las sesiones activas
                }
            }
        }

        //agregamos tiempo para apps que todavia estan abiertas hasta el tiempo actual
        activeApps.forEach { (app, startTime) ->
            val usageDuration = endTimeAhora - startTime
            statsMapBD.getOrPut(app) { 0L }
            statsMapBD[app] = (statsMapBD[app] ?: 0L) + usageDuration
        }

        // 🔹 Imprimir resultado
        Log.d("MioParametro", "statsMapBD $statsMapBD")

        return statsMapBD

    }

    // 🔹 Obtener el tiempo de uso de hoy incluso el tiempo de apps abiertas ♻️♻️♻️
    suspend fun getTiempoDeUsoHoy(): Map<String, Long> {

        val startTimeHoy = Fun.getTimeAtras(0) // "Hoy a las 00:00:00"
        val endTimeAhora = System.currentTimeMillis() // tiempo actual

        Log.d("getStatsHoy", "getStatsHoy ${Fun.dateFormat.format(startTimeHoy)}")

        val statsMapBD = mutableMapOf<String, Long>()

        // 🔹 Obtener eventos de la base de datos
        val usageEventsBD = getEventsFromDatabase(startTimeHoy, endTimeAhora)

        // 🔹 Variables para manejar múltiples apps en primer plano
        val activeApps = mutableMapOf<String, Long>() // packageName -> timestamp de inicio

        // Mapa para almacenar los últimos dos eventos de cada app(para calcular el uso de la app que tienen conciquencia rara de los eventos)
        val lastTwoEventsByApp = mutableMapOf<String, MutableList<Int>>()

        // 🔹 Procesar eventos en orden cronológico
        val sortedEvents = usageEventsBD.sortedBy { it.timestamp }
        sortedEvents.forEach { event ->

            //if (event.packageName != "org.telegram.messenger") return@forEach

            // formamos 2 ultimos eventos por app
            val eventsForApp = lastTwoEventsByApp.getOrPut(event.packageName) { mutableListOf() }
            // Agrega el evento actual
            eventsForApp.add(event.eventType)
            // Limita a tres eventos
            if (eventsForApp.size > 3) eventsForApp.removeAt(0) // Remueve el evento más antiguo si hay más de tres

            val eventTime = event.timestamp

            when (event.eventType) {
                //1
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    // Registrar el inicio de la sesión de esta app
                    if (activeApps[event.packageName] == null) {
                        activeApps[event.packageName] = eventTime
                    }
                }
                //23
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    // si primero evento ACTIVITY_STOPPED es significa que la app estaba abierta, por eso sumamos desde el inicio hasta eventTime
                    if (eventsForApp.size == 1) {
                        val usageDuration = eventTime - startTimeHoy
                        statsMapBD.getOrPut(event.packageName) { 0L }
                        statsMapBD[event.packageName] =
                            (statsMapBD[event.packageName] ?: 0L) + usageDuration
                    }
                    // si ultimos dos eventos es ACTIVITY_PAUSED,ACTIVITY_RESUMED es incorrecto por eso no cerrar la sesion
                    if (!(lastTwoEventsByApp[event.packageName]?.get(0) == UsageEvents.Event.ACTIVITY_PAUSED &&
                                lastTwoEventsByApp[event.packageName]?.get(1) == UsageEvents.Event.ACTIVITY_RESUMED)
                    ) {
                        // Cerrar la sesión de esta app si estaba activa
                        activeApps[event.packageName]?.let { startTime ->
                            val usageDuration = eventTime - startTime
                            statsMapBD.getOrPut(event.packageName) { 0L }
                            statsMapBD[event.packageName] =
                                (statsMapBD[event.packageName] ?: 0L) + usageDuration
                            activeApps.remove(event.packageName) // Eliminar de la lista de activas
                        }
                    }
                }
                // 26, 27
                UsageEvents.Event.DEVICE_SHUTDOWN, UsageEvents.Event.DEVICE_STARTUP -> {
                    // Cerrar las sesiónes de TODAS las apps activas
                    activeApps.forEach { (app, startTime) ->
                        val usageDuration = eventTime - startTime
                        statsMapBD.getOrPut(app) { 0L }
                        statsMapBD[app] = (statsMapBD[app] ?: 0L) + usageDuration
                    }
                    activeApps.clear() // Limpiar todas las sesiones activas
                }
            }
        }

        //agregamos tiempo para apps que todavia estan abiertas hasta el tiempo actual
        activeApps.forEach { (app, startTime) ->
            val usageDuration = endTimeAhora - startTime
            statsMapBD.getOrPut(app) { 0L }
            statsMapBD[app] = (statsMapBD[app] ?: 0L) + usageDuration
        }

        // 🔹 Imprimir resultado
        Log.d("MioParametro", "statsMapBD $statsMapBD")

        return statsMapBD

    }

    //=========================


    //========= Eventos ===================================

    suspend fun saveUsEventsUltimoMesToDatabase() {
        Log.d("MioParametro", "🎈saveUsEventsUltimoMesToDatabase Start...")
        // 🔹 Obtener el último timestamp guardado
        val lastSavedTimestamp = usageEventDao.getLastTimestamp() ?: 0L
        val startTimeMesAtras = Fun.getTimeAtras(30)
        var startTime = startTimeMesAtras
        if (lastSavedTimestamp < startTimeMesAtras) {
            usageEventDao.deleteOldEvents(startTimeMesAtras)
        } else {
            startTime = lastSavedTimestamp + 1 // 🔹 Evita traer los eventos repetidos de pasado
        }
        Log.d("MioParametro", "startTime ${Fun.dateFormat.format(startTime)}")

        val endTime = System.currentTimeMillis() // 🔹 Hasta el momento actual

        val eventList = mutableListOf<UsageEventEntity>()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val usageEvents = usageStatsManager?.queryEvents(startTime, endTime) ?: return
        val event = UsageEvents.Event()


        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)

            if (event.packageName == "com.google.android.youtube") {
                Log.w(
                    "MioParametro",
                    "Even teventType: ${event.eventType}, pkg: ${event.packageName}, timestamp: ${
                        Fun.dateFormat.format(event.timeStamp)
                    } ms:${event.timeStamp}"
                )
            }

            when (event.eventType) {
                UsageEvents.Event.DEVICE_SHUTDOWN, //26
                UsageEvents.Event.DEVICE_STARTUP, //27
                UsageEvents.Event.ACTIVITY_RESUMED, //1
                UsageEvents.Event.ACTIVITY_PAUSED, //2
                UsageEvents.Event.ACTIVITY_STOPPED, //23
                    -> {
                    // Guardar en lista para insertar en Room
                    eventList.add(
                        UsageEventEntity(
                            packageName = event.packageName,
                            eventType = event.eventType,
                            timestamp = event.timeStamp
                        )
                    )
                }
            }
        }

        usageEventDao.insertAll(eventList) // Guardar solo eventos nuevos
        Log.d("MioParametro", "🎈🎈saveUsEventsUltimoMesToDatabase End")
    }

    suspend fun getEventsFromDatabase(startTime: Long, endTime: Long): List<UsageEventEntity> {
        saveUsEventsUltimoMesToDatabase()
        return usageEventDao.getEvents(startTime, endTime)
    }

    suspend fun getEventsFromDatabase(
        startTime: Long,
        endTime: Long,
        listaApps: List<String>
    ): List<UsageEventEntity> {
        saveUsEventsUltimoMesToDatabase()
        return usageEventDao.getEvents(startTime, endTime, listaApps)
    }


    //========= Stats ===================================

    suspend fun getStatsFromDatabase(startTime: Long, endTime: Long): List<UsageStatsEntity> {
        saveUsStatsUltimaSemanaToDatabase()
        return usageStatsDao.getAllUsageStats()
    }

    suspend fun saveUsStatsUltimaSemanaToDatabase() {
        Log.d("MioParametro", "🕧saveUsStatsUltimoSemanaToDatabase Start...")
        // 🔹 Obtener el último timestamp guardado
        val lastSavedDia = usageStatsDao.getLastDia() ?: 0L

        val startTimeSemanaAtras = Fun.getTimeAtras(7)
        var startTime = startTimeSemanaAtras
        if (lastSavedDia < startTimeSemanaAtras) {
            usageStatsDao.deleteOldUsageStats(startTimeSemanaAtras)
        } else {
            startTime = lastSavedDia + (24 * 60 * 60 * 1000) // +1 día en milisegundos
        }
        Log.d("MioParametro", "startDia ${Fun.dateFormat.format(startTime)}")

        val endTime = Fun.getTimeAtras(0) // 🔹 Hasta el dia actual a la medianoche

        val statsEntityList = mutableListOf<UsageStatsEntity>()

        val usageStats = getUsageStatsEnSistema(startTime, endTime)

        usageStats.forEach { pkgName ->
            pkgName.value.forEach { dia ->
                statsEntityList.add(
                    UsageStatsEntity(
                        packageName = pkgName.key,
                        dia = dia.key,
                        usageDuration = dia.value
                    )
                )
            }
        }

        usageStatsDao.insertAllUsageStats(statsEntityList) // Guardar stats nuevos a BD

        Log.d("MioParametro", "🕧🕧saveUsStatsUltimoSemanaToDatabase End")
    }

    fun getUsageStatsEnSistema(
        startTime: Long,
        endTime: Long
    ): MutableMap<String, MutableMap<Long, Long>> {

        Log.d("MioParametro", "startTime ${Fun.dateFormat.format(startTime)}")
        Log.d("MioParametro", "endTime ${Fun.dateFormat.format(endTime)}")

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val usageStatsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        ) ?: emptyList()

        // Crear un mapa para almacenar el tiempo de uso por día para cada App
        val statsMapBD = mutableMapOf<String, MutableMap<Long, Long>>()

        usageStatsList.forEach { stats ->
            //if (stats.packageName != "com.whatsapp.w4b") return@forEach
            if (stats.lastTimeUsed !in startTime..endTime) return@forEach

            val calendarDay = Calendar.getInstance().apply {
                timeInMillis = stats.lastTimeUsed // Convertir timestamp a Calendar
                set(Calendar.HOUR_OF_DAY, 0) // Fijar la hora a medianoche
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val day = calendarDay.timeInMillis

            Log.d(
                "MioParametro",
                "stats ${stats.packageName} totalTimeInForeground:${stats.totalTimeInForeground} lastTimeUsed:${
                    Fun.dateFormat.format(stats.lastTimeUsed)
                } dia:${Fun.dateFormat.format(day)}"
            )

            val usageDuration = stats.totalTimeInForeground
            val appUsageMap = statsMapBD.getOrPut(stats.packageName) { mutableMapOf() }
            appUsageMap[day] = (appUsageMap[day] ?: 0L) + usageDuration
        }

        // 🔹 Imprimir resultado
        Log.d("MioParametro", "statsMapBD $statsMapBD")

        return statsMapBD

    }


    //======================================================================

    fun cancelarCorrutinas() {
        coroutineScope.cancel() // Cancelar todas las corrutinas cuando ya no sea necesario
    }

    //======================================================================


}
