package com.ursolgleb.controlparental.handlers

import android.content.Context
import android.util.Log
import com.ursolgleb.controlparental.data.local.AppDataRepository
import com.ursolgleb.controlparental.data.remote.RemoteDataRepository
import com.ursolgleb.controlparental.data.remote.models.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager moderno para sincronización basada en eventos
 */
@Singleton
class EventSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localRepo: AppDataRepository,
    private val remoteRepo: RemoteDataRepository,
    private val syncHandler: SyncHandler,
    private val syncStateManager: SyncStateManager
) {
    companion object {
        private const val TAG = "EventSyncManager"
        private const val PREF_LAST_EVENT_ID = "last_sync_event_id"
        private const val PREF_LAST_SYNC_TIME = "last_sync_time"
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
            syncStateManager.setSyncState(SyncState.SYNCING, "Iniciando sincronización...")
            
            val deviceInfo = localRepo.getDeviceInfoOnce() ?: return Result.failure(Exception("No device info"))
            val deviceId = deviceInfo.deviceId
            
            // 1. Verificar estado de sincronización
            syncStateManager.setSyncState(SyncState.SYNCING, "Verificando estado...")
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
            syncStateManager.setSyncState(SyncState.SYNCING, "Obteniendo eventos del servidor...")
            var lastEventId = getLastEventId()
            var hasMore = true
            var retryCount = 0
            var totalEventsReceived = 0
            
            while (hasMore && retryCount < 5) {
                try {
                    val serverEvents = remoteRepo.getEvents(
                        deviceId = deviceId, 
                        lastEventId = lastEventId,
                        types = "horario,app" // Enviar como una sola cadena
                    )
                    
                    if (serverEvents.events.isNotEmpty()) {
                        Log.d(TAG, "Received ${serverEvents.events.size} events from server")
                        totalEventsReceived += serverEvents.events.size
                        syncStateManager.setSyncState(SyncState.SYNCING, "Aplicando ${serverEvents.events.size} eventos...")
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
                syncStateManager.setSyncState(SyncState.SYNCING, "Enviando ${localEvents.size} eventos locales...")
                Log.d(TAG, "Sending ${localEvents.size} local events")
                val response = remoteRepo.postEvents(PostEventsRequest(deviceId, localEvents))
                if (response.isSuccessful) {
                    clearLocalEventFlags()
                }
            }
            
            // Guardar tiempo de última sincronización exitosa
            setLastSyncTime()
            
            syncStateManager.setSyncState(
                SyncState.SUCCESS, 
                "Sincronización completada (${totalEventsReceived} recibidos, ${localEvents.size} enviados)"
            )
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sync error", e)
            syncStateManager.setSyncState(SyncState.ERROR, "Error: ${e.message}")
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
        val idHorario = event.entity_id.toLongOrNull() ?: run {
            Log.e(TAG, "Received horario event with invalid entity_id: ${event.entity_id}")
            return
        }

        when (event.action) {
            "create", "update" -> {
                val data = event.data ?: run {
                    Log.e(TAG, "Received horario update event without data for id: $idHorario")
                    return
                }

                // Construir el DTO directamente desde el mapa, igual que hacemos con las apps.
                val horarioDto = HorarioDto(
                    deviceId = event.deviceId, // Usar el deviceId del evento como fuente de verdad
                    idHorario = idHorario,     // Usar el entity_id del evento como fuente de verdad
                    nombreDeHorario = data["nombreDeHorario"] as? String ?: "",
                    diasDeSemana = (data["diasDeSemana"] as? List<*>)?.mapNotNull { (it as? Double)?.toInt() } ?: emptyList(),
                    horaInicio = data["horaInicio"] as? String ?: "00:00",
                    horaFin = data["horaFin"] as? String ?: "23:59",
                    isActive = data["isActive"] as? Boolean ?: false
                )

                horarioDto.toEntity().let {
                    Log.d(TAG, "Applying server event: CREATE/UPDATE Horario ${it.idHorario}")
                    localRepo.insertHorariosEntidades(listOf(it))
                }
            }
            "delete" -> {
                Log.d(TAG, "Applying server event: DELETE Horario $idHorario")
                localRepo.deleteHorarioByIdHorario(idHorario, event.deviceId).await()
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

        // 1. Procesar horarios pendientes de eliminación
        syncHandler.getDeletedHorarioIds().forEach { id ->
            events.add(EventDto(
                entity_type = "horario",
                entity_id = id,
                action = "delete",
                timestamp = now
            ))
        }

        // 2. Procesar horarios pendientes de creación/actualización
        syncHandler.getPendingHorarioIds().forEach { id ->
            val idAsLong = id.toLongOrNull()
            if (idAsLong != null) {
                localRepo.getHorarioByIdOnce(idAsLong, deviceId)?.let { horario ->
                    events.add(EventDto(
                        entity_type = "horario",
                        entity_id = horario.idHorario.toString(),
                        action = "update", // El servidor usará updateOrCreate
                        data = horario.toDto().toMap(),
                        timestamp = now
                    ))
                }
            }
        }
        
        // 3. Procesar apps pendientes de eliminación
        syncHandler.getDeletedAppIds().forEach { packageName ->
            events.add(EventDto(
                entity_type = "app",
                entity_id = packageName,
                action = "delete",
                timestamp = now
            ))
        }

        // 4. Procesar apps pendientes de creación/actualización
        syncHandler.getPendingAppIds().forEach { packageName ->
            localRepo.getAppByPackageNameOnce(packageName, deviceId)?.let { app ->
                events.add(EventDto(
                    entity_type = "app",
                    entity_id = app.packageName,
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
        
        map["idHorario"] = idHorario
        deviceId?.let { map["deviceId"] = it }
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
    
    private fun setLastSyncTime() {
        prefs.edit().putLong(PREF_LAST_SYNC_TIME, System.currentTimeMillis()).apply()
    }
    
    private fun clearLocalEventFlags() {
        syncHandler.clearDeletedHorarioIds()
        syncHandler.clearPendingHorarioIds()
        syncHandler.clearDeletedAppIds()
        syncHandler.clearPendingAppIds()
    }
} 