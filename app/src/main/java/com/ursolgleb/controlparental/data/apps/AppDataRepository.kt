package com.ursolgleb.controlparental.data.apps

import android.app.usage.UsageEvents
import kotlinx.coroutines.runBlocking
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.DeadObjectException
import com.ursolgleb.controlparental.data.apps.dao.AppDao
import com.ursolgleb.controlparental.data.apps.dao.HorarioDao
import com.ursolgleb.controlparental.data.apps.dao.UsageEventDao
import com.ursolgleb.controlparental.data.apps.dao.UsageStatsDao
import com.ursolgleb.controlparental.data.apps.entities.AppEntity
import com.ursolgleb.controlparental.data.apps.entities.HorarioEntity
import com.ursolgleb.controlparental.data.apps.entities.UsageEventEntity
import com.ursolgleb.controlparental.data.apps.entities.UsageStatsEntity
import com.ursolgleb.controlparental.utils.AppsFun
import com.ursolgleb.controlparental.utils.Fun
import com.ursolgleb.controlparental.utils.Launcher
import com.ursolgleb.controlparental.utils.Logger
import com.ursolgleb.controlparental.utils.StatusApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppDataRepository @Inject constructor(
    val appDatabase: AppDatabase,//
    @ApplicationContext val context: Context,
    private val newAppsProvider: NewAppsProvider,//
    private val usageTimeProvider: UsageTimeProvider,//
    private val usageStatsProvider: UsageStatsProvider//
) {

    val defLauncher = Launcher.getDefaultLauncherPackageName(context)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val appDao: AppDao = appDatabase.appDao()//
    private val horarioDao: HorarioDao = appDatabase.horarioDao()//

    private var isInicieDeLecturaTermina = false

    private val lockInicieDelecturaDeBD = Mutex()
    private val lockUpdateBDApps = Mutex()
    private val lockUpdateTiempoDeUso = Mutex()
    private val lockUpdateTiempoDeUsoUnaApp = Mutex()
    private val dbLock = Mutex()

    val todosAppsFlow = MutableStateFlow<List<AppEntity>>(emptyList())

    val blockedAppsFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    val horarioAppsFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    val disponAppsFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    val horariosFlow = MutableStateFlow<List<HorarioEntity>>(emptyList())

    val todosAppsMenosBloqueadosFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    var todosAppsMenosHorarioFlow = MutableStateFlow<List<AppEntity>>(emptyList())
    val todosAppsMenosDisponFlow = MutableStateFlow<List<AppEntity>>(emptyList())

    val mutexUpdateBDAppsStateFlow = MutableStateFlow(false)
    val mostrarBottomSheetActualizadaFlow = MutableStateFlow(false)

    private fun actualizarFlows(apps: List<AppEntity>, horarios: List<HorarioEntity>) {
        todosAppsFlow.value = apps
        blockedAppsFlow.value = apps.filter { it.appStatus == StatusApp.BLOQUEADA.desc }
        horarioAppsFlow.value = apps.filter { it.appStatus == StatusApp.HORARIO.desc }
        disponAppsFlow.value = apps.filter { it.appStatus == StatusApp.DISPONIBLE.desc }
        todosAppsMenosBloqueadosFlow.value =
            apps.filter { it.appStatus != StatusApp.BLOQUEADA.desc }
        todosAppsMenosHorarioFlow.value = apps.filter { it.appStatus != StatusApp.HORARIO.desc }
        todosAppsMenosDisponFlow.value = apps.filter { it.appStatus != StatusApp.DISPONIBLE.desc }
        horariosFlow.value = horarios
    }

    fun inicieDelecturaDeBD() {
        val locked = lockInicieDelecturaDeBD.tryLock()
        if (!locked) {
            Logger.warn(context, "AppDataRepository", "inicieDelecturaDeBD ya está en ejecución")
            return
        }

        scope.launch {
            try {
                dbLock.withLock {
                    Logger.info(context, "AppDataRepository", "Iniciando inicieDelecturaDeBD")
                    val apps = appDao.getAllApps().first()
                    val horarios = horarioDao.getAllHorarios().first()
                    actualizarFlows(apps, horarios)
                    Logger.info(context, "AppDataRepository", "Apps y horarios cargados")
                }
            } catch (e: DeadObjectException) {
                Logger.error(context, "AppDataRepository", "DeadObjectException: ${e.message}", e)
            } catch (e: Exception) {
                Logger.error(
                    context,
                    "AppDataRepository",
                    "Error en inicieDelecturaDeBD: ${e.message}",
                    e
                )
            } finally {
                cargarAppsEnBackgroundDesdeBD()
                isInicieDeLecturaTermina = true
                Logger.info(context, "AppDataRepository", "inicieDelecturaDeBD finalizada")
                if (locked) lockInicieDelecturaDeBD.unlock()
                updateBDApps()
            }
        }
    }

    private fun cargarAppsEnBackgroundDesdeBD() {
        scope.launch {
            appDao.getAllApps()
                .combine(horarioDao.getAllHorarios()) { apps, horarios ->
                    apps to horarios
                }
                .collect { (apps, horarios) ->
                    actualizarFlows(apps, horarios)
                    Logger.info(
                        context,
                        "AppDataRepository",
                        "Flows actualizados en background: apps=${apps.size}, horarios=${horarios.size}"
                    )
                }
        }
    }

    fun updateBDApps() {
        if (!isInicieDeLecturaTermina) {
            inicieDelecturaDeBD()
            return
        }

        scope.launch {
            val locked = lockUpdateBDApps.tryLock()
            if (!locked) {
                mutexUpdateBDAppsStateFlow.value = true
                Logger.warn(context, "AppDataRepository", "updateBDApps ya está en ejecución")
                return@launch
            }

            mutexUpdateBDAppsStateFlow.value = true
            try {
                dbLock.withLock {
                    Logger.info(context, "AppDataRepository", "Ejecutando updateBDApps")
                    Logger.info(context, "AppDataRepository", "Start getNuevasAppsEnSistema...")
                    val appsNuevas = newAppsProvider.getNuevasAppsEnSistema()
                    Logger.info(context, "AppDataRepository", "Finish getNuevasAppsEnSistema.")
                    if (appsNuevas.isNotEmpty()) {
                        Logger.info(context, "AppDataRepository", "Start addListaAppsBD...")
                        addListaAppsBD(appsNuevas)
                        Logger.info(context, "AppDataRepository", "Finish addListaAppsBD.")
                    }
                }
            } catch (e: Exception) {
                Logger.error(context, "AppDataRepository", "Error en updateBDApps: ${e.message}", e)
            } finally {
                lockUpdateBDApps.unlock()
                mutexUpdateBDAppsStateFlow.value = false
                Logger.info(context, "AppDataRepository", "updateBDApps finalizada")
            }
        }
    }

    suspend fun addListaAppsBD(appsNuevas: List<ApplicationInfo>) {
        if (appsNuevas.isEmpty()) return
        val pm = context.packageManager

        Logger.info(context, "AppDataRepository", "Start crear getTiempoDeUsoSeconds...")
        val tiempoDeUso = usageTimeProvider.getTiempoDeUsoHoy(appsNuevas) { it.packageName }
        Logger.info(context, "AppDataRepository", "Finish crear getTiempoDeUsoSeconds.")

        Logger.info(context, "AppDataRepository", "Start crear nuevasEntidades...")
        val nuevasEntidades = appsNuevas.map { app ->
            val drawable = app.loadIcon(pm)
            val bitmap = AppsFun.drawableToBitmap(drawable)
            val entretenimiento = app.category in listOf(
                ApplicationInfo.CATEGORY_GAME,
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO
            )
            var status = if (entretenimiento) StatusApp.HORARIO.desc else StatusApp.BLOQUEADA.desc
            status = StatusApp.BLOQUEADA.desc
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
        Logger.info(context, "AppDataRepository", "Finish crear nuevasEntidades.")

        try {
            Logger.info(
                context,
                "AppDataRepository",
                "Start insertListaApps nuevasEntidades a BD..."
            )
            appDao.insertListaApps(nuevasEntidades)
            mostrarBottomSheetActualizadaFlow.value = true
            Logger.info(
                context,
                "AppDataRepository",
                "Finish insertListaApps nuevasEntidades a BD."
            )
        } catch (e: Exception) {
            Logger.error(
                context,
                "AppDataRepository",
                "Error al insertar apps en la BD: ${e.message}",
                e
            )
        } finally {
            Logger.info(
                context,
                "AppDataRepository",
                "Nueva Lista App insertada a AppsBD: ${appsNuevas.size}"
            )
        }
    }

    fun addAppsASiempreBloqueadasBD(appsNuevas: List<AppEntity>) {
        if (appsNuevas.isEmpty()) return
        val appsBloqueadas = appsNuevas.mapNotNull { app ->
            if (app.appStatus != StatusApp.BLOQUEADA.desc) {
                app.copy(appStatus = StatusApp.BLOQUEADA.desc, dailyUsageLimitMinutes = 0)
            } else null
        }
        if (appsBloqueadas.isEmpty()) return

        scope.launch {
            try {
                dbLock.withLock {
                    Logger.info(
                        context,
                        "AppDataRepository",
                        "addAppsASiempreBloqueadasBD agregando a BD..."
                    )
                    appDao.insertListaApps(appsBloqueadas)
                    mostrarBottomSheetActualizadaFlow.value = true
                }
            } catch (e: Exception) {
                Logger.error(
                    context,
                    "AppDataRepository",
                    "Error en addAppsASiempreBloqueadasBD: ${e.message}",
                    e
                )
            } finally {
                Logger.info(
                    context,
                    "AppDataRepository",
                    "Nueva Lista Apps bloqueadas: ${appsBloqueadas.size}"
                )
            }
        }
    }

    fun addAppsASiempreDisponiblesBD(appsNuevas: List<AppEntity>) {
        if (appsNuevas.isEmpty()) return
        val appsDispon = appsNuevas.mapNotNull { app ->
            if (app.appStatus != StatusApp.DISPONIBLE.desc) {
                app.copy(appStatus = StatusApp.DISPONIBLE.desc, dailyUsageLimitMinutes = 0)
            } else null
        }
        if (appsDispon.isEmpty()) return

        scope.launch {
            try {
                dbLock.withLock {
                    Logger.info(
                        context,
                        "AppDataRepository",
                        "addAppsASiempreDisponiblesBD agregando a BD..."
                    )
                    appDao.insertListaApps(appsDispon)
                    mostrarBottomSheetActualizadaFlow.value = true
                }
            } catch (e: Exception) {
                Logger.error(
                    context,
                    "AppDataRepository",
                    "Error en addAppsASiempreDisponiblesBD: ${e.message}",
                    e
                )
            } finally {
                Logger.info(
                    context,
                    "AppDataRepository",
                    "Nueva Lista Apps disponibles: ${appsDispon.size}"
                )
            }
        }
    }

    fun addAppsAHorarioBD(appsNuevas: List<AppEntity>) {
        if (appsNuevas.isEmpty()) return
        val appsHorario = appsNuevas.mapNotNull { app ->
            if (app.appStatus != StatusApp.HORARIO.desc) {
                app.copy(appStatus = StatusApp.HORARIO.desc)
            } else null
        }
        if (appsHorario.isEmpty()) return

        scope.launch {
            try {
                dbLock.withLock {
                    Logger.info(context, "AppDataRepository", "addAppsAHorarioBD agregando a BD...")
                    appDao.insertListaApps(appsHorario)
                    mostrarBottomSheetActualizadaFlow.value = true
                }
            } catch (e: Exception) {
                Logger.error(
                    context,
                    "AppDataRepository",
                    "Error en addAppsAHorarioBD: ${e.message}",
                    e
                )
            } finally {
                Logger.info(
                    context,
                    "AppDataRepository",
                    "Nueva Lista Apps horario: ${appsHorario.size}"
                )
            }
        }
    }

    fun addNuevoPkgBD(pkgName: String) {
        val listaApplicationInfo = listOfNotNull(AppsFun.getApplicationInfo(context, pkgName))
        scope.launch { addListaAppsBD(listaApplicationInfo) }
    }

    suspend fun siEsNuevoPkg(pkg: String): Boolean {
        // 0 ➞ no existe, es nuevo
        return appDao.countByPackage(pkg) == 0
    }

    fun addHorarioBD(horario: HorarioEntity) {
        scope.launch {
            try {
                horarioDao.insertHorario(horario)
                Logger.info(context, "AppDataRepository", "Horario insertado en BD: $horario")
            } catch (e: Exception) {
                Logger.error(
                    context,
                    "AppDataRepository",
                    "Error al insertar horario en la BD: ${e.message}",
                    e
                )
            }
        }
    }

    fun updateTiempoDeUsoUnaApp(pkgName: String) = scope.launch {
        val locked = lockUpdateTiempoDeUsoUnaApp.tryLock()
        if (!locked) {
            mutexUpdateBDAppsStateFlow.value = true
            Logger.warn(context, "AppDataRepository", "renovarTiempoUsoAppHoy ya está en ejecución")
            return@launch
        }
        try {
            dbLock.withLock {
                mutexUpdateBDAppsStateFlow.value = true
                val tiempoDeUsoMap = mutableMapOf<String, Long>()
                val listPkgName = listOf(pkgName)
                val tiempoDeUso = usageTimeProvider.getTiempoDeUsoHoy(listPkgName) { it }
                listPkgName.forEach { app ->
                    tiempoDeUsoMap[app] = tiempoDeUso[app] ?: 0L
                }
                if (tiempoDeUsoMap.isNotEmpty()) appDao.updateUsageTimeHoy(tiempoDeUsoMap)
                Logger.info(
                    context,
                    "AppDataRepository",
                    "renovarTiempoUsoAppHoy actualizado: $pkgName"
                )
            }
        } catch (e: Exception) {
            Logger.error(
                context,
                "AppDataRepository",
                "Error en renovarTiempoUsoAppHoy: ${e.message}",
                e
            )
        } finally {
            lockUpdateTiempoDeUsoUnaApp.unlock()
            mutexUpdateBDAppsStateFlow.value = false
            Logger.info(context, "AppDataRepository", "renovarTiempoUsoAppHoy finalizada")
        }
    }

    fun updateTiempoUsoAppsHoy() = scope.launch {
        val locked = lockUpdateTiempoDeUso.tryLock()
        if (!locked) {
            mutexUpdateBDAppsStateFlow.value = true
            Logger.warn(context, "AppDataRepository", "updateTiempoUsoAppsHoy ya está en ejecución")
            return@launch
        }

        try {
            dbLock.withLock {
                mutexUpdateBDAppsStateFlow.value = true
                Logger.info(context, "AppDataRepository", "Empezando updateTiempoUsoAppsHoy")
                val tiempoDeUsoMapHoy = mutableMapOf<String, Long>()
                val appsBD = todosAppsFlow.value
                val tiempoHoyMap = usageTimeProvider.getTiempoDeUsoHoy(appsBD) { it.packageName }
                appsBD.forEach { appBD ->
                    val uso = tiempoHoyMap[appBD.packageName] ?: 0L
                    if (appBD.usageTimeToday != uso) {
                        tiempoDeUsoMapHoy[appBD.packageName] = uso
                    }
                }
                if (tiempoDeUsoMapHoy.isNotEmpty()) appDao.updateUsageTimeHoy(tiempoDeUsoMapHoy)
                Logger.info(
                    context,
                    "AppDataRepository",
                    "updateTiempoUsoAppsHoy aplicado para ${tiempoDeUsoMapHoy.size} apps"
                )
            }
        } catch (e: Exception) {
            Logger.error(
                context,
                "AppDataRepository",
                "Error en updateTiempoUsoAppsHoy: ${e.message}",
                e
            )
        } finally {
            lockUpdateTiempoDeUso.unlock()
            mutexUpdateBDAppsStateFlow.value = false
            Logger.info(context, "AppDataRepository", "updateTiempoUsoAppsHoy finalizada")
        }
    }

    fun cancelarCorrutinas() {
        scope.cancel()
    }
}
