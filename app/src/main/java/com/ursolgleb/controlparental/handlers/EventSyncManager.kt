package com.ursolgleb.controlparental.handlers

import android.content.Context
import android.util.Log
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.data.remote.RemoteDataRepository
import com.ursolgleb.controlparental.data.remote.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import java.io.EOFException
import retrofit2.HttpException

/**
 * Manager moderno para sincronización basada en eventos
 */
@Singleton
class EventSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localRepo: AppDataRepository,
    private val remoteRepo: RemoteDataRepository,
    private val syncHandler: SyncHandler
) {
    companion object {
        private const val TAG = "EventSyncManager"
        private const val PREF_LAST_EVENT_ID = "last_sync_event_id"
    }
    
    private val prefs = context.getSharedPreferences("event_sync", Context.MODE_PRIVATE)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    
    /**
     * Sincronizar con el servidor usando el sistema de eventos
     */
    suspend fun sync(): Result<Unit> {
        return try {
            val deviceInfo = localRepo.getDeviceInfoOnce() ?: return Result.failure(Exception("No device info"))
            val deviceId = deviceInfo.deviceId
            
            // 1. Verificar estado de sincronización
            val status = try {
                remoteRepo.getSyncStatus(deviceId)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting sync status, continuing anyway", e)
                null
            }
            
            if (status != null) {
                Log.d(TAG, "Sync status: $status")
            }
            
            // 2. Obtener eventos del servidor
            var lastEventId = getLastEventId()
            var hasMore = true
            var retryCount = 0
            
            while (hasMore && retryCount < 5) {
                try {
                    val serverEvents = remoteRepo.getEvents(deviceId, lastEventId)
                    
                    if (serverEvents.events.isNotEmpty()) {
                        Log.d(TAG, "Received ${serverEvents.events.size} events from server")
                        applyServerEvents(serverEvents.events)
                        lastEventId = serverEvents.lastEventId
                        setLastEventId(lastEventId)
                    }
                    
                    hasMore = serverEvents.hasMore
                    retryCount = 0 // Reset retry count on success
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching events, retrying...", e)
                    retryCount++
                    if (retryCount >= 5) throw e
                    kotlinx.coroutines.delay(1000) // Wait before retry
                }
            }
            
            // 3. Enviar eventos locales pendientes
            val localEvents = collectLocalEvents(deviceId)
            if (localEvents.isNotEmpty()) {
                Log.d(TAG, "Sending ${localEvents.size} local events")
                val response = remoteRepo.postEvents(PostEventsRequest(deviceId, localEvents))
                if (response.isSuccessful) {
                    clearLocalEventFlags()
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            Result.failure(e)
        }
    }
    
    /**
     * Aplicar eventos recibidos del servidor
     */
    private suspend fun applyServerEvents(events: List<SyncEvent>) {
        events.forEach { event ->
            try {
                when (event.entity_type) {
                    "horario" -> applyHorarioEvent(event)
                    "app" -> applyAppEvent(event)
                    "device" -> applyDeviceEvent(event)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error applying event ${event.id}", e)
            }
        }
    }
    
    /**
     * Aplicar evento de horario
     */
    private suspend fun applyHorarioEvent(event: SyncEvent) {
        when (event.action) {
            "create", "update" -> {
                event.data?.let { data ->
                    val horarioDto = HorarioDto(
                        deviceId = data["deviceId"] as? String ?: "",
                        idHorario = (data["idHorario"] as? Number)?.toLong() ?: 0L,
                        nombreDeHorario = data["nombreDeHorario"] as? String ?: "",
                        diasDeSemana = (data["diasDeSemana"] as? List<*>)?.mapNotNull { 
                            (it as? Number)?.toInt() 
                        } ?: emptyList(),
                        horaInicio = data["horaInicio"] as? String ?: "",
                        horaFin = data["horaFin"] as? String ?: "",
                        isActive = data["isActive"] as? Boolean ?: false
                    )
                    
                    horarioDto.toEntity()?.let { entity ->
                        localRepo.insertHorariosEntidades(listOf(entity))
                    }
                }
            }
            "delete" -> {
                val idHorario = event.entity_id.toLongOrNull() ?: return
                val deviceId = event.deviceId
                localRepo.deleteHorarioByIdHorario(idHorario, deviceId).await()
            }
        }
    }
    
    /**
     * Aplicar evento de app
     */
    private suspend fun applyAppEvent(event: SyncEvent) {
        when (event.action) {
            "create", "update" -> {
                event.data?.let { data ->
                    val appDto = AppDto(
                        deviceId = data["deviceId"] as? String ?: "",
                        packageName = data["packageName"] as? String ?: event.entity_id,
                        appName = data["appName"] as? String ?: "",
                        appIcon = null, // Los íconos no se sincronizan en eventos
                        appCategory = data["appCategory"] as? String,
                        contentRating = data["contentRating"] as? String,
                        isSystemApp = data["isSystemApp"] as? Boolean ?: false,
                        usageTimeToday = (data["usageTimeToday"] as? Number)?.toLong() ?: 0L,
                        timeStempUsageTimeToday = (data["timeStempUsageTimeToday"] as? Number)?.toLong() ?: 0L,
                        appStatus = data["appStatus"] as? String ?: "DEFAULT",
                        dailyUsageLimitMinutes = (data["dailyUsageLimitMinutes"] as? Number)?.toInt() ?: 0
                    )
                    
                    appDto.toEntity()?.let { entity ->
                        localRepo.insertAppsEntidades(listOf(entity))
                    }
                }
            }
            "delete" -> {
                val packageName = event.entity_id
                val deviceId = event.deviceId
                localRepo.deleteAppByPackageName(packageName, deviceId)
            }
        }
    }
    
    /**
     * Aplicar evento de dispositivo
     */
    private suspend fun applyDeviceEvent(event: SyncEvent) {
        // Por ahora solo manejamos actualizaciones de dispositivo
        when (event.action) {
            "update" -> {
                // Los datos del dispositivo se actualizan en otros lugares
                Log.d(TAG, "Device update event received but not processed here")
            }
        }
    }
    
    /**
     * Recolectar eventos locales pendientes
     */
    private suspend fun collectLocalEvents(deviceId: String): List<EventDto> {
        val events = mutableListOf<EventDto>()
        val now = dateFormat.format(Date())
        
        // Si hay cambios de horarios pendientes
        if (syncHandler.isPushHorarioPendiente()) {
            val horarios = localRepo.horariosFlow.first()
            
            horarios.forEach { horario ->
                events.add(EventDto(
                    entityType = "horario",
                    entityId = horario.idHorario.toString(),
                    action = "update",
                    data = horario.toDto().toMap(),
                    timestamp = now
                ))
            }
        }
        
        // Si hay cambios de apps pendientes
        if (syncHandler.isPushAppsPendiente()) {
            val apps = localRepo.todosAppsFlow.value
            
            apps.forEach { app ->
                events.add(EventDto(
                    entityType = "app",
                    entityId = app.packageName,
                    action = "update",
                    data = app.toDto().toMap(),
                    timestamp = now
                ))
            }
        }
        
        return events
    }
    
    /**
     * Parsear HorarioDto desde Map
     */
    private fun parseHorarioFromData(data: Map<String, Any>): HorarioDto {
        return HorarioDto(
            deviceId = data["deviceId"] as? String ?: "",
            idHorario = (data["idHorario"] as? Number)?.toLong() ?: 0,
            nombreDeHorario = data["nombreDeHorario"] as? String ?: "",
            diasDeSemana = (data["diasDeSemana"] as? List<*>)?.mapNotNull { 
                (it as? Number)?.toInt() 
            } ?: emptyList(),
            horaInicio = data["horaInicio"] as? String ?: "",
            horaFin = data["horaFin"] as? String ?: "",
            isActive = data["isActive"] as? Boolean ?: false
        )
    }
    
    /**
     * Parsear AppDto desde Map
     */
    private fun parseAppFromData(data: Map<String, Any>): AppDto {
        return AppDto(
            deviceId = data["deviceId"] as? String ?: "",
            packageName = data["packageName"] as? String ?: "",
            appName = data["appName"] as? String ?: "",
            appIcon = null, // Los íconos se manejan por separado
            appCategory = data["appCategory"] as? String,
            contentRating = data["contentRating"] as? String,
            isSystemApp = data["isSystemApp"] as? Boolean ?: false,
            usageTimeToday = (data["usageTimeToday"] as? Number)?.toLong() ?: 0L,
            timeStempUsageTimeToday = (data["timeStempUsageTimeToday"] as? Number)?.toLong() ?: 0,
            appStatus = data["appStatus"] as? String ?: "DISPONIBLE",
            dailyUsageLimitMinutes = (data["dailyUsageLimitMinutes"] as? Number)?.toInt() ?: 0
        )
    }
    
    /**
     * Convertir DTO a Map para enviar
     */
    private fun HorarioDto.toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        
        deviceId?.let { map["deviceId"] = it }
        map["idHorario"] = idHorario
        map["nombreDeHorario"] = nombreDeHorario
        map["diasDeSemana"] = diasDeSemana
        horaInicio?.let { map["horaInicio"] = it }
        horaFin?.let { map["horaFin"] = it }
        map["isActive"] = isActive
        
        return map
    }
    
    /**
     * Convertir AppDto a Map para enviar
     */
    private fun AppDto.toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        
        // Agregar solo valores no nulos o con valores por defecto
        deviceId?.let { map["deviceId"] = it }
        packageName?.let { map["packageName"] = it }
        appName?.let { map["appName"] = it }
        map["isSystemApp"] = isSystemApp
        usageTimeToday?.let { map["usageTimeToday"] = it }
        map["timeStempUsageTimeToday"] = timeStempUsageTimeToday
        appStatus?.let { map["appStatus"] = it }
        dailyUsageLimitMinutes?.let { map["dailyUsageLimitMinutes"] = it }
        appCategory?.let { map["appCategory"] = it }
        contentRating?.let { map["contentRating"] = it }
        
        return map
    }
    
    private fun getLastEventId(): Long {
        return prefs.getLong(PREF_LAST_EVENT_ID, 0)
    }
    
    private fun setLastEventId(eventId: Long) {
        prefs.edit().putLong(PREF_LAST_EVENT_ID, eventId).apply()
    }
    
    private fun clearLocalEventFlags() {
        syncHandler.setPushHorarioPendiente(false)
        syncHandler.setPushAppsPendiente(false)
    }
    
    /**
     * Push cambios locales pendientes al servidor
     */
    suspend fun pushPendingChanges() {
        Log.d(TAG, "pushPendingChanges called")
        val device = localRepo.getDeviceInfoOnce()?.toDto() ?: return
        
        try {
            // Push horarios si hay cambios pendientes
            val horarioPendiente = syncHandler.isPushHorarioPendiente()
            Log.d(TAG, "Checking horario pendiente: $horarioPendiente")
            
            if (horarioPendiente) {
                Log.d(TAG, "Pushing pending horarios...")
                val horariosDto = localRepo.horariosFlow.first().map { it.toDto() }
                Log.d(TAG, "Found ${horariosDto.size} horarios to push")
                if (horariosDto.isNotEmpty()) {
                    remoteRepo.pushHorarios(horariosDto)
                } else {
                    remoteRepo.deleteHorarios(listOf(device.deviceId.toString()))
                }
                syncHandler.setPushHorarioPendiente(false)
            }
            
            // Push apps si hay cambios pendientes
            val appsPendiente = syncHandler.isPushAppsPendiente()
            Log.d(TAG, "Checking apps pendiente: $appsPendiente")
            
            if (appsPendiente) {
                Log.d(TAG, "Pushing pending apps...")
                val appsDto = localRepo.todosAppsFlow.value.map { it.toDto() }
                Log.d(TAG, "Found ${appsDto.size} apps to push")
                if (appsDto.isNotEmpty()) {
                    remoteRepo.pushApps(appsDto)
                } else {
                    remoteRepo.deleteApps(listOf(device.deviceId.toString()))
                }
                syncHandler.setPushAppsPendiente(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pushing pending changes", e)
            throw e
        }
    }

    suspend fun syncWithServer(): Boolean {
        Log.d(TAG, "syncWithServer called at ${System.currentTimeMillis()}")
        return try {
            val device = localRepo.getDeviceInfoOnce()?.toDto()
            if (device == null) {
                Log.e(TAG, "No device info available")
                return false
            }
            
            // 1. Sincronizar información del dispositivo
            try {
                remoteRepo.pushDevice(device)
                Log.d(TAG, "Device info synced: $device")
            } catch (e: EOFException) {
                Log.w(TAG, "EOFException syncing device, continuing...")
            }
            
            // 2. Verificar estado de sincronización
            val status = try {
                remoteRepo.getSyncStatus(device.deviceId.toString())
            } catch (e: EOFException) {
                Log.w(TAG, "EOFException getting sync status, continuing...")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error getting sync status, continuing anyway", e)
                null
            }
            
            if (status != null) {
                Log.d(TAG, "Sync status: $status")
            }
            
            // 3. Obtener el último ID de evento procesado
            val lastEventId = getLastEventId()
            Log.d(TAG, "Last processed event ID: $lastEventId")
            
            // 4. Obtener y procesar eventos pendientes
            var hasMoreEvents = true
            var currentLastEventId = lastEventId
            var totalEventsProcessed = 0
            var retryCount = 0
            val maxRetries = 3
            
            while (hasMoreEvents && retryCount < maxRetries) {
                try {
                    val eventsResponse = remoteRepo.getEvents(
                        deviceId = device.deviceId.toString(),
                        lastEventId = currentLastEventId,
                        types = listOf("horario", "app")
                    )
                    
                    Log.d(TAG, "Fetched ${eventsResponse.events.size} events, hasMore: ${eventsResponse.hasMore}")
                    
                    // Procesar eventos
                    for (event in eventsResponse.events) {
                        try {
                            applyServerEvent(event)
                            totalEventsProcessed++
                        } catch (e: Exception) {
                            Log.e(TAG, "Error applying event ${event.id}: ${e.message}")
                        }
                    }
                    
                    // Actualizar el último ID procesado
                    if (eventsResponse.events.isNotEmpty()) {
                        currentLastEventId = eventsResponse.lastEventId
                        setLastEventId(currentLastEventId)
                    }
                    
                    hasMoreEvents = eventsResponse.hasMore
                    retryCount = 0 // Reset retry count on success
                    
                } catch (e: EOFException) {
                    retryCount++
                    Log.w(TAG, "EOFException getting events (attempt $retryCount/$maxRetries): ${e.message}")
                    if (retryCount < maxRetries) {
                        kotlinx.coroutines.delay(1000L * retryCount)
                        continue
                    } else {
                        Log.e(TAG, "Max retries reached for events sync")
                        break
                    }
                } catch (e: HttpException) {
                    Log.e(TAG, "HTTP error getting events: ${e.code()} ${e.message()}")
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Unexpected error getting events", e)
                    break
                }
            }
            
            Log.d(TAG, "Event sync completed. Total events processed: $totalEventsProcessed")
            
            // 5. Si es la primera sincronización (lastEventId era 0), hacer sync completo
            if (lastEventId == 0L && totalEventsProcessed == 0) {
                Log.d(TAG, "First sync detected, performing full sync")
                try {
                    performFullSync(device)
                } catch (e: EOFException) {
                    Log.e(TAG, "EOFException during full sync, will retry next time", e)
                    return false
                }
            }
            
            // 6. Actualizar tiempo de uso de apps
            localRepo.updateTiempoUsoAppsHoy().await()
            
            true
        } catch (e: EOFException) {
            Log.e(TAG, "EOFException in sync", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            false
        }
    }
    
    private suspend fun applyServerEvent(event: SyncEvent) {
        Log.d(TAG, "Applying server event: ${event.action} ${event.entity_type} ${event.entity_id}")
        
        when (event.entity_type) {
            "horario" -> applyHorarioEvent(event)
            "app" -> applyAppEvent(event)
            "device" -> applyDeviceEvent(event)
            else -> Log.w(TAG, "Unknown entity type: ${event.entity_type}")
        }
    }
    
    private suspend fun performFullSync(device: DeviceDto) {
        try {
            // Primero asegurar que el dispositivo existe en la BD local
            Log.d(TAG, "Ensuring device exists in local DB")
            localRepo.saveDeviceInfo().await()
            
            // Sincronizar horarios
            Log.d(TAG, "Performing full horarios sync")
            val remoteHorariosResponse = remoteRepo.fetchHorarios(
                deviceId = device.deviceId,
                lastSync = null, // Forzar full sync
                knownIds = null
            )
            val remoteHorarios = remoteHorariosResponse.data
            
            Log.d(TAG, "Received ${remoteHorarios?.size ?: 0} horarios from full sync.")

            if (!remoteHorarios.isNullOrEmpty()) {
                localRepo.deleteAllHorarios().await()
                val horariosEntity = remoteHorarios.mapNotNull { it.toEntity() }
                if (horariosEntity.isNotEmpty()) {
                    localRepo.insertHorariosEntidades(horariosEntity)
                    Log.d(TAG, "Inserted ${horariosEntity.size} horarios")
                }
            } else {
                Log.d(TAG, "No remote horarios found, clearing local.")
                localRepo.deleteAllHorarios().await()
            }
            
            // Sincronizar apps
            Log.d(TAG, "Performing full apps sync")
            // Primera pasada: sincronizar sin íconos para obtener todos los datos rápidamente
            val remoteApps = remoteRepo.fetchApps(device.deviceId, includeIcons = false)
            if (remoteApps.isNotEmpty()) {
                localRepo.deleteAllApps().await()
                val appsEntity = remoteApps.mapNotNull { it.toEntity() }
                if (appsEntity.isNotEmpty()) {
                    localRepo.insertAppsEntidades(appsEntity)
                    Log.d(TAG, "Inserted ${appsEntity.size} apps")
                }
                
                // TODO: En una futura versión, sincronizar íconos por separado
                // Esto evitaría los problemas de JSON truncado
            }
        } catch (e: Exception) {
            Log.e(TAG, "Full sync failed", e)
            throw e
        }
    }
} 