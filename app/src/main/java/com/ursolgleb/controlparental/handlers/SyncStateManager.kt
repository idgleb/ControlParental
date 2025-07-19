package com.ursolgleb.controlparental.handlers

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager dedicado para el estado de sincronización
 * Sin dependencias circulares
 */
@Singleton
class SyncStateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    syncHandler: SyncHandler
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Estado de sincronización
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    // Información adicional del estado
    private val _syncInfo = MutableStateFlow("")
    val syncInfo: StateFlow<String> = _syncInfo.asStateFlow()

    // Contador de eventos pendientes (reactivo)
    val pendingEventsCount: StateFlow<Int> = combine(
        syncHandler.pendingHorarioIdsFlow,
        syncHandler.deletedHorarioIdsFlow,
        syncHandler.pendingAppIdsFlow,
        syncHandler.deletedAppIdsFlow
    ) { pendingH, deletedH, pendingA, deletedA ->
        val total = pendingH.size + deletedH.size + pendingA.size + deletedA.size

        // Actualizar el estado si hay eventos pendientes
        if (total > 0 && _syncState.value == SyncState.IDLE) {
            _syncState.value = SyncState.PENDING_EVENTS
            _syncInfo.value =
                "Eventos pendientes: ${pendingH.size + deletedH.size} horarios, ${pendingA.size + deletedA.size} apps"
        } else if (total == 0 && _syncState.value == SyncState.PENDING_EVENTS) {
            _syncState.value = SyncState.IDLE
            _syncInfo.value = ""
        }

        total
    }.stateIn(
        scope,
        SharingStarted.Eagerly,
        0
    )

    // Estado combinado para UI
    val syncStatusText: StateFlow<String> = combine(
        syncState,
        syncInfo,
        pendingEventsCount
    ) { state, info, pendingCount ->
        when (state) {
            SyncState.IDLE -> {
                val lastSync = getFormattedLastSyncTime()
                if (pendingCount > 0) {
                    "📤 $pendingCount eventos pendientes • $lastSync"
                } else {
                    "✅ Sincronizado • $lastSync"
                }
            }

            SyncState.SYNCING -> "🔄 $info"
            SyncState.SUCCESS -> "✅ $info"
            SyncState.ERROR -> "❌ $info"
            SyncState.PENDING_EVENTS -> "📤 $info"
        }
    }.stateIn(
        scope,
        SharingStarted.WhileSubscribed(5000),
        "Iniciando..."
    )

    fun setSyncState(state: SyncState, info: String = "") {
        _syncState.value = state
        _syncInfo.value = info
    }

    fun getLastSyncTime(): Long {
        val prefs = context.createDeviceProtectedStorageContext()
            .getSharedPreferences("event_sync", Context.MODE_PRIVATE)
        return prefs.getLong("last_sync_time", 0)
    }

    private fun getFormattedLastSyncTime(): String {
        val lastSync = getLastSyncTime()

        if (lastSync == 0L) return "Nunca sincronizado"

        val now = System.currentTimeMillis()
        val diff = now - lastSync

        return when {
            diff < 60_000 -> "Hace menos de 1 minuto"
            diff < 3600_000 -> "Hace ${diff / 60_000} minutos"
            diff < 86400_000 -> "Hace ${diff / 3600_000} horas"
            else -> "Hace ${diff / 86400_000} días"
        }
    }
} 