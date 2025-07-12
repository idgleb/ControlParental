package com.ursolgleb.controlparental.data.local

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.DeadObjectException
import android.util.Log
import android.os.Build
import android.os.BatteryManager
import com.ursolgleb.controlparental.data.local.dao.AppDao
import com.ursolgleb.controlparental.data.local.dao.HorarioDao
import com.ursolgleb.controlparental.data.local.dao.DeviceDao
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.HorarioEntity
import com.ursolgleb.controlparental.data.local.entities.DeviceEntity
import com.ursolgleb.controlparental.data.local.providers.NewAppsProvider
import com.ursolgleb.controlparental.data.local.providers.UsageStatsProvider
import com.ursolgleb.controlparental.data.local.providers.UsageTimeProvider
import com.ursolgleb.controlparental.utils.AppsFun
import com.ursolgleb.controlparental.utils.Launcher
import com.ursolgleb.controlparental.utils.Logger
import com.ursolgleb.controlparental.utils.StatusApp
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import com.ursolgleb.controlparental.handlers.SyncHandler
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import com.ursolgleb.controlparental.data.common.Resource
import com.ursolgleb.controlparental.data.common.networkBoundResource
import com.ursolgleb.controlparental.data.remote.RemoteDataRepository
import com.ursolgleb.controlparental.data.remote.models.toEntity
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import com.ursolgleb.controlparental.handlers.SyncStateManager
import com.ursolgleb.controlparental.data.auth.local.DeviceAuthLocalDataSource

@Singleton
class AppDataRepository @Inject constructor(
    val appDatabase: AppDatabase,
    @ApplicationContext val context: Context,
    private val newAppsProvider: NewAppsProvider,
    private val usageTimeProvider: UsageTimeProvider,
    private val usageStatsProvider: UsageStatsProvider,
    private val syncHandler: SyncHandler,
    private val remoteDataRepository: RemoteDataRepository,
    private val syncStateManager: SyncStateManager,
    private val deviceAuthLocalDataSource: DeviceAuthLocalDataSource
) {

    var currentPkg: String? = null

    val defLauncher = Launcher.getDefaultLauncherPackageName(context)

    val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val appDao: AppDao = appDatabase.appDao()
    private val horarioDao: HorarioDao = appDatabase.horarioDao()
    private val deviceDao: DeviceDao = appDatabase.deviceDao()

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
    val syncStatusFlow = MutableStateFlow("inicio..")

    fun getOrCreateDeviceId(): String {
        // Intentar obtener el deviceId del sistema de autenticación
        val authDeviceId = deviceAuthLocalDataSource.getDeviceId()
        if (authDeviceId != null) {
            return authDeviceId
        }
        
        // Si no hay deviceId, generar uno nuevo permanente
        // Este mismo ID se usará para la autenticación cuando el dispositivo se registre
        val newDeviceId = java.util.UUID.randomUUID().toString()
        
        // Guardar el deviceId en el sistema de autenticación para uso futuro
        deviceAuthLocalDataSource.saveDeviceId(newDeviceId)
        
        Logger.info(context, "AppDataRepository", 
            "Generado nuevo deviceId permanente: $newDeviceId")
        return newDeviceId
    }

    private fun actualizarFlows(
        apps: List<AppEntity>,
        horarios: List<HorarioEntity>,
    ) {
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
                cargarSyncStatus()
                isInicieDeLecturaTermina = true
                Logger.info(context, "AppDataRepository", "inicieDelecturaDeBD finalizada")
                if (locked) lockInicieDelecturaDeBD.unlock()
                updateBDApps()
            }
        }
    }

    private fun cargarSyncStatus() {
        // Conectar el estado de sincronización de SyncStateManager con syncStatusFlow
        scope.launch {
            syncStateManager.syncStatusText.collect { status ->
                syncStatusFlow.value = status
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
        val deviceId = getOrCreateDeviceId()
        val nuevasEntidades = appsNuevas.map { app ->
            val drawable = app.loadIcon(pm)
            val bitmap = AppsFun.drawableToBitmap(drawable)
            val status = StatusApp.BLOQUEADA.desc
            val timestampActual = System.currentTimeMillis()
            AppEntity(
                packageName = app.packageName,
                deviceId = deviceId,
                appName = if (app.loadLabel(pm).toString()
                        .isEmpty()
                ) app.packageName else app.loadLabel(pm).toString(),
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
            nuevasEntidades.forEach { syncHandler.addPendingAppId(it.packageName) }
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
                    appsBloqueadas.forEach { syncHandler.addPendingAppId(it.packageName) }
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
                    appsDispon.forEach { syncHandler.addPendingAppId(it.packageName) }
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
                    appsHorario.forEach { syncHandler.addPendingAppId(it.packageName) }
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

    fun deleteAllApps(): Deferred<Unit> = scope.async {
        try {
            val apps = appDao.getAllApps().first()
            appDao.deleteAllApps()
            apps.forEach { syncHandler.addDeletedAppId(it.packageName) }
        } catch (e: Exception) {
            Logger.error(
                context,
                "AppDataRepository",
                "Error al eliminar todas las apps en la BD: ${e.message}",
                e
            )
        }
    }

    suspend fun insertAppsEntidades(apps: List<AppEntity>) {
        if (apps.isEmpty()) return
        try {
            dbLock.withLock {
                appDao.insertListaApps(apps)
            }
        } catch (e: Exception) {
            Logger.error(
                context,
                "AppDataRepository",
                "Error insertAppsEntidades: ${e.message}",
                e
            )
        }
    }

    suspend fun addHorarioBD(horario: HorarioEntity) {
        try {
            horarioDao.insertHorario(horario)
            syncHandler.addPendingHorarioId(horario.idHorario)
            Logger.info(
                context,
                "AppDataRepository",
                "Horario insertado en BD: $horario"
            )
        } catch (e: Exception) {
            Logger.error(
                context,
                "AppDataRepository",
                "Error al insertar horario en la BD: ${e.message}",
                e
            )
        }
    }

    fun deleteHorarioBD(horario: HorarioEntity) {
        scope.launch {
            try {
                horarioDao.deleteHorario(horario)
                Logger.info(context, "AppDataRepository", "Horario eliminado en BD: $horario")
                syncHandler.addDeletedHorarioId(horario.idHorario)
            } catch (e: Exception) {
                Logger.error(
                    context,
                    "AppDataRepository",
                    "Error al eliminar horario en la BD: ${e.message}",
                    e
                )
            }
        }
    }

    fun deleteAllHorarios(): Deferred<Unit> = scope.async {
        try {
            horarioDao.deleteAllHorarios()
        } catch (e: Exception) {
            Logger.error(
                context,
                "AppDataRepository",
                "Error al eliminar todos los horarios en la BD: ${e.message}",
                e
            )
        }
    }

    fun deleteHorarioByIdHorario(idHorario: Long, deviceId: String): Deferred<Unit> = scope.async {
        try {
            horarioDao.deleteByIdHorario(idHorario, deviceId)
            Logger.info(context, "AppDataRepository", "Horario eliminado por idHorario: $idHorario, deviceId: $deviceId")
        } catch (e: Exception) {
            Logger.error(
                context,
                "AppDataRepository",
                "Error al eliminar horario por idHorario: ${e.message}",
                e
            )
        }
    }

    suspend fun insertHorariosEntidades(horarios: List<HorarioEntity>) {
        if (horarios.isEmpty()) return
        try {
            dbLock.withLock {
                horarioDao.insertHorarios(horarios)
            }
        } catch (e: Exception) {
            Logger.error(
                context,
                "AppDataRepository",
                "Error insertHorariosEntidades: ${e.message}",
                e
            )
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

    fun updateTiempoUsoAppsHoy(): Deferred<Unit> = scope.async {
        val locked = lockUpdateTiempoDeUso.tryLock()
        if (!locked) {
            mutexUpdateBDAppsStateFlow.value = true
            Logger.warn(context, "AppDataRepository", "updateTiempoUsoAppsHoy ya está en ejecución")
            return@async
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


    fun saveDeviceInfo(): Deferred<Unit> = scope.async {
        try {
            // Siempre usar el deviceId del sistema de autenticación
            val deviceId = getOrCreateDeviceId()
            val model = "${Build.MANUFACTURER} ${Build.MODEL}"
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            // Obtener el device existente si hay uno
            val existingDevice = deviceDao.getDeviceOnce()
            
            val entity = if (existingDevice != null) {
                // Actualizar el existente manteniendo algunos campos
                existingDevice.copy(
                    deviceId = deviceId, // Siempre usar el deviceId del auth
                    model = model,
                    batteryLevel = battery,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                // Crear uno nuevo
                DeviceEntity(
                    deviceId = deviceId, 
                    model = model, 
                    batteryLevel = battery
                )
            }
            
            // Si el deviceId cambió, necesitamos eliminar el antiguo primero
            if (existingDevice != null && existingDevice.deviceId != deviceId) {
                deviceDao.deleteAll()
            }
            
            deviceDao.replace(entity)
            
            // Marcar el dispositivo como pendiente de sincronización
            syncHandler.markDeviceUpdatePending()
            
            Logger.info(context, "AppDataRepository", "Device info guardada: $entity")
        } catch (e: Exception) {
            Logger.error(
                context,
                "AppDataRepository",
                "Error guardando device info: ${e.message}",
                e
            )
        }
    }

    suspend fun getDeviceInfoOnce(): DeviceEntity? {
        return try {
            // Siempre obtener el deviceId del sistema de autenticación
            val authDeviceId = getOrCreateDeviceId()
            
            // Obtener el device actual de la BD
            val existingDevice = deviceDao.getDeviceOnce()
            
            if (existingDevice != null) {
                // Si existe pero tiene un deviceId diferente, actualizarlo
                if (existingDevice.deviceId != authDeviceId) {
                    Logger.info(context, "AppDataRepository", 
                        "Actualizando deviceId de ${existingDevice.deviceId} a $authDeviceId")
                    
                    // Crear nuevo entity con el deviceId correcto
                    val updatedDevice = existingDevice.copy(
                        deviceId = authDeviceId,
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    // Eliminar el antiguo y crear el nuevo
                    deviceDao.deleteAll()
                    deviceDao.insertIgnore(updatedDevice)
                    
                    return updatedDevice
                }
                return existingDevice
            }
            
            // Si no existe, crear uno nuevo con el deviceId del sistema de autenticación
            val model = "${Build.MANUFACTURER} ${Build.MODEL}"
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val battery = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val entity = DeviceEntity(
                deviceId = authDeviceId, 
                model = model, 
                batteryLevel = battery
            )
            
            // Guardar en la BD
            deviceDao.insertIgnore(entity)
            Logger.info(context, "AppDataRepository", "Device info creada y guardada: $entity")
            
            entity
        } catch (e: Exception) {
            Logger.error(
                context,
                "AppDataRepository",
                "Error obteniendo device info: ${e.message}",
                e
            )
            // Como último recurso, crear un device temporal
            val deviceId = getOrCreateDeviceId()
            DeviceEntity(
                deviceId = deviceId,
                model = "${Build.MANUFACTURER} ${Build.MODEL}",
                batteryLevel = 100
            )
        }
    }

    fun getDeviceFlow() = deviceDao.getDevice()
    
    /**
     * Actualiza la información del dispositivo en la base de datos
     */
    suspend fun updateDeviceInfo(device: DeviceEntity) {
        try {
            deviceDao.replace(device)
            Logger.info(context, "AppDataRepository", "Device info actualizada: $device")
        } catch (e: Exception) {
            Logger.error(
                context,
                "AppDataRepository",
                "Error actualizando device info: ${e.message}",
                e
            )
        }
    }
    
    suspend fun deleteAppByPackageName(packageName: String, deviceId: String) {
        try {
            dbLock.withLock {
                appDao.deleteAppByPackageName(packageName, deviceId)
                syncHandler.addDeletedAppId(packageName)
                Log.d("AppDataRepository", "App eliminada: $packageName, notificada para sync.")
            }
        } catch (e: Exception) {
            Logger.error(context, "AppDataRepository", "Error deleteAppByPackageName: ${e.message}", e)
        }
    }

    suspend fun getAppByPackageNameOnce(packageName: String, deviceId: String): AppEntity? {
        return try {
            appDao.getAppByPackageNameOnce(packageName, deviceId)
        } catch (e: Exception) {
            Logger.error(context, "AppDataRepository", "Error getAppByPackageNameOnce: ${e.message}", e)
            null
        }
    }

    suspend fun getHorarioByIdOnce(idHorario: Long, deviceId: String): HorarioEntity? {
        return try {
            horarioDao.getHorarioByIdOnce(idHorario, deviceId)
        } catch (e: Exception) {
            Logger.error(context, "AppDataRepository", "Error getHorarioByIdOnce: ${e.message}", e)
            null
        }
    }

    fun cancelarCorrutinas() {
        scope.cancel()
    }

    fun getHorarios(deviceId: String): Flow<Resource<List<HorarioEntity>>> = networkBoundResource(
        query = {
            horarioDao.getAllHorarios()
        },
        fetch = {
            val response = remoteDataRepository.api.getHorarios(deviceId)
            if (response.isSuccessful && response.body() != null) {
                // Adaptamos la respuesta: devolvemos solo la lista de DTOs
                Response.success(response.body()!!.data)
            } else {
                // Si la respuesta no es exitosa, la pasamos tal cual para que se maneje el error
                Response.error(response.code(), response.errorBody()!!)
            }
        },
        saveFetchResult = { remoteHorarios ->
            dbLock.withLock {
                // Sincronización inteligente: solo aplicar cambios necesarios
                Log.d("AppDataRepo", "Iniciando sincronización inteligente de horarios")
                
                // 1. Obtener horarios locales actuales
                val localHorarios = horarioDao.getHorariosByDeviceIdOnce(deviceId)
                val localIds = localHorarios.map { it.idHorario }.toSet()
                val remoteIds = remoteHorarios.map { it.idHorario }.toSet()
                
                Log.d("AppDataRepo", "Horarios locales: ${localIds.size}, remotos: ${remoteIds.size}")
                
                // 2. Identificar horarios a eliminar (existen localmente pero no en el servidor)
                val idsToDelete = localIds - remoteIds
                if (idsToDelete.isNotEmpty()) {
                    Log.d("AppDataRepo", "Eliminando ${idsToDelete.size} horarios que ya no existen en el servidor: $idsToDelete")
                    horarioDao.deleteHorariosByIds(idsToDelete.toList())
                }
                
                // 3. Identificar horarios nuevos y actualizados
                val horariosToInsert = mutableListOf<HorarioEntity>()
                val horariosToUpdate = mutableListOf<HorarioEntity>()
                
                remoteHorarios.forEach { remoteHorario ->
                    val entity = remoteHorario.toEntity()
                    val localHorario = localHorarios.find { it.idHorario == remoteHorario.idHorario }
                    
                    if (localHorario == null) {
                        // Horario nuevo
                        horariosToInsert.add(entity)
                    } else if (hasChanges(localHorario, entity)) {
                        // Horario existente con cambios
                        horariosToUpdate.add(entity)
                    }
                    // Si no hay cambios, no hacer nada
                }
                
                // 4. Aplicar inserciones
                if (horariosToInsert.isNotEmpty()) {
                    Log.d("AppDataRepo", "Insertando ${horariosToInsert.size} horarios nuevos")
                    horarioDao.insertHorarios(horariosToInsert)
                }
                
                // 5. Aplicar actualizaciones
                if (horariosToUpdate.isNotEmpty()) {
                    Log.d("AppDataRepo", "Actualizando ${horariosToUpdate.size} horarios modificados")
                    horariosToUpdate.forEach { horarioDao.updateHorario(it) }
                }
                
                val totalChanges = idsToDelete.size + horariosToInsert.size + horariosToUpdate.size
                Log.d("AppDataRepo", "Sincronización completada. Total de cambios: $totalChanges")
            }
        },
        shouldFetch = { localData ->
            // Solo sincronizar si:
            // 1. No hay datos locales
            // 2. Es una sincronización inicial forzada
            // 3. Hay una flag indicando que hay cambios en el servidor
            localData == null || 
            localData.isEmpty() || 
            hasPendingServerChanges("horario")
        } 
    )
    
    /**
     * Compara dos horarios para detectar si hay cambios
     */
    private fun hasChanges(local: HorarioEntity, remote: HorarioEntity): Boolean {
        return local.nombreDeHorario != remote.nombreDeHorario ||
               local.diasDeSemana != remote.diasDeSemana ||
               local.horaInicio != remote.horaInicio ||
               local.horaFin != remote.horaFin ||
               local.isActive != remote.isActive
    }
    
    /**
     * Obtiene las aplicaciones del dispositivo con sincronización de red
     * Similar a getHorarios pero para apps
     */
    fun getApps(deviceId: String): Flow<Resource<List<AppEntity>>> = networkBoundResource(
        query = {
            appDao.getAllApps()
        },
        fetch = {
            // Usar la API paginada de apps
            val response = remoteDataRepository.api.getApps(
                deviceId = deviceId,
                limit = 1000, // Obtener todas las apps
                includeIcons = false // Los íconos los manejamos localmente
            )
            Response.success(response.data)
        },
        saveFetchResult = { remoteApps ->
            dbLock.withLock {
                // Sincronización inteligente de apps
                Log.d("AppDataRepo", "Iniciando sincronización inteligente de apps")
                
                // 1. Obtener apps locales actuales del dispositivo
                val localApps = appDao.getAllApps().first().filter { it.deviceId == deviceId }
                val localPackages = localApps.map { it.packageName }.toSet()
                val remotePackages = remoteApps.mapNotNull { it.packageName }.toSet()
                
                Log.d("AppDataRepo", "Apps locales: ${localPackages.size}, remotas: ${remotePackages.size}")
                
                // 2. Identificar apps a eliminar (ya no están en el servidor)
                val packagesToDelete = localPackages - remotePackages
                if (packagesToDelete.isNotEmpty()) {
                    Log.d("AppDataRepo", "Eliminando ${packagesToDelete.size} apps que ya no existen en el servidor")
                    packagesToDelete.forEach { pkg ->
                        appDao.deleteAppByPackageName(pkg, deviceId)
                    }
                }
                
                // 3. Identificar apps nuevas y actualizadas
                val appsToInsert = mutableListOf<AppEntity>()
                val appsToUpdate = mutableListOf<AppEntity>()
                
                remoteApps.forEach { remoteApp ->
                    val entity = remoteApp.toEntity()
                    if (entity != null) {
                        val localApp = localApps.find { it.packageName == remoteApp.packageName }
                        
                        if (localApp == null) {
                            // App nueva - pero preservar el ícono local si existe
                            val existingIcon = getLocalAppIcon(remoteApp.packageName!!)
                            if (existingIcon != null) {
                                appsToInsert.add(entity.copy(appIcon = existingIcon))
                            } else {
                                appsToInsert.add(entity)
                            }
                        } else if (hasAppChanges(localApp, entity)) {
                            // App existente con cambios - preservar ícono local
                            appsToUpdate.add(entity.copy(appIcon = localApp.appIcon))
                        }
                    }
                }
                
                // 4. Aplicar cambios
                if (appsToInsert.isNotEmpty() || appsToUpdate.isNotEmpty()) {
                    val allAppsToSave = appsToInsert + appsToUpdate
                    Log.d("AppDataRepo", "Guardando ${allAppsToSave.size} apps (${appsToInsert.size} nuevas, ${appsToUpdate.size} actualizadas)")
                    appDao.insertListaApps(allAppsToSave)
                }
                
                val totalChanges = packagesToDelete.size + appsToInsert.size + appsToUpdate.size
                Log.d("AppDataRepo", "Sincronización de apps completada. Total de cambios: $totalChanges")
            }
        },
        shouldFetch = { localApps ->
            // Sincronizar si:
            // 1. No hay datos locales
            // 2. Hay cambios pendientes en el servidor
            // 3. Los datos están obsoletos (más de 5 minutos)
            localApps == null || 
            localApps.isEmpty() || 
            hasPendingServerChanges("app") ||
            isDataStale()
        }
    )
    
    /**
     * Verifica si hay cambios pendientes en el servidor para un tipo de entidad
     */
    private fun hasPendingServerChanges(entityType: String): Boolean {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val pendingChanges = prefs.getStringSet("pending_server_changes", emptySet()) ?: emptySet()
        return pendingChanges.contains(entityType)
    }
    
    /**
     * Marca que hay cambios pendientes del servidor para un tipo de entidad
     */
    fun markServerChanges(entityType: String, hasPending: Boolean) {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val pendingChanges = prefs.getStringSet("pending_server_changes", emptySet())?.toMutableSet() ?: mutableSetOf()
        
        if (hasPending) {
            pendingChanges.add(entityType)
        } else {
            pendingChanges.remove(entityType)
        }
        
        prefs.edit().putStringSet("pending_server_changes", pendingChanges).apply()
    }
    
    /**
     * Compara dos apps para detectar si hay cambios (excluyendo el ícono)
     */
    private fun hasAppChanges(local: AppEntity, remote: AppEntity): Boolean {
        return local.appName != remote.appName ||
               local.appStatus != remote.appStatus ||
               local.dailyUsageLimitMinutes != remote.dailyUsageLimitMinutes ||
               local.appCategory != remote.appCategory ||
               local.contentRating != remote.contentRating ||
               local.isSystemApp != remote.isSystemApp
    }
    
    /**
     * Obtiene el ícono de una app instalada localmente
     */
    private fun getLocalAppIcon(packageName: String): android.graphics.Bitmap? {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val drawable = appInfo.loadIcon(context.packageManager)
            AppsFun.drawableToBitmap(drawable)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Verifica si los datos locales están desactualizados
     */
    private fun isDataStale(): Boolean {
        // Considerar datos obsoletos después de 5 minutos
        val lastSyncTime = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            .getLong("last_apps_sync", 0)
        return System.currentTimeMillis() - lastSyncTime > 5 * 60 * 1000
    }

    fun isTimeSyncNeeded(): Boolean {
        val lastSyncTime = syncStateManager.getLastSyncTime()
        return System.currentTimeMillis() - lastSyncTime > 5 * 60 * 1000
    }
}
