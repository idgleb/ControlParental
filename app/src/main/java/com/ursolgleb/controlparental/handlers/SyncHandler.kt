package com.ursolgleb.controlparental.handlers

import android.content.Context
import android.util.Log
import com.ursolgleb.controlparental.data.local.AppDataRepository
import com.ursolgleb.controlparental.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Provider
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class SyncHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDataRepository: Provider<AppDataRepository>,
) {

    private val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    // Flows reactivos para los IDs pendientes
    private val _pendingHorarioIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingHorarioIdsFlow: StateFlow<Set<String>> = _pendingHorarioIds.asStateFlow()
    
    private val _deletedHorarioIds = MutableStateFlow<Set<String>>(emptySet())
    val deletedHorarioIdsFlow: StateFlow<Set<String>> = _deletedHorarioIds.asStateFlow()
    
    private val _pendingAppIds = MutableStateFlow<Set<String>>(emptySet())
    val pendingAppIdsFlow: StateFlow<Set<String>> = _pendingAppIds.asStateFlow()
    
    private val _deletedAppIds = MutableStateFlow<Set<String>>(emptySet())
    val deletedAppIdsFlow: StateFlow<Set<String>> = _deletedAppIds.asStateFlow()
    
    private val _deviceUpdatePending = MutableStateFlow(false)
    val deviceUpdatePendingFlow: StateFlow<Boolean> = _deviceUpdatePending.asStateFlow()
    
    init {
        // Cargar valores iniciales desde SharedPreferences
        _pendingHorarioIds.value = getPendingHorarioIds()
        _deletedHorarioIds.value = getDeletedHorarioIds()
        _pendingAppIds.value = getPendingAppIds()
        _deletedAppIds.value = getDeletedAppIds()
        _deviceUpdatePending.value = isDeviceUpdatePending()
    }

    // --- Métodos para pendientes de creación/actualización de Horarios ---
    fun addPendingHorarioId(id: Long) {
        val current = getPendingHorarioIds()
        val newSet = current.toMutableSet()
        newSet.add(id.toString())
        prefs.edit() { putStringSet("pending_horario_ids", newSet) }
        _pendingHorarioIds.value = newSet
        Log.d("SyncHandler", "Added pending horario ID: $id.")
    }

    fun getPendingHorarioIds(): Set<String> {
        return prefs.getStringSet("pending_horario_ids", emptySet()) ?: emptySet()
    }

    fun clearPendingHorarioIds() {
        prefs.edit() { remove("pending_horario_ids") }
        _pendingHorarioIds.value = emptySet()
        Log.d("SyncHandler", "Cleared pending horario IDs.")
    }

    // --- Métodos para pendientes de eliminación de Horarios ---
    fun addDeletedHorarioId(id: Long) {
        val current = getDeletedHorarioIds()
        val newSet = current.toMutableSet()
        newSet.add(id.toString())
        prefs.edit() { putStringSet("deleted_horario_ids", newSet) }
        _deletedHorarioIds.value = newSet
        Log.d("SyncHandler", "Added deleted horario ID: $id.")
    }

    fun getDeletedHorarioIds(): Set<String> {
        return prefs.getStringSet("deleted_horario_ids", emptySet()) ?: emptySet()
    }

    fun clearDeletedHorarioIds() {
        prefs.edit() { remove("deleted_horario_ids") }
        _deletedHorarioIds.value = emptySet()
        Log.d("SyncHandler", "Cleared deleted horario IDs.")
    }

    // --- Métodos para pendientes de creación/actualización de Apps ---
    fun addPendingAppId(packageName: String) {
        val current = getPendingAppIds()
        val newSet = current.toMutableSet()
        newSet.add(packageName)
        prefs.edit() { putStringSet("pending_app_ids", newSet) }
        _pendingAppIds.value = newSet
        Log.d("SyncHandler", "Added pending app ID: $packageName.")
    }

    fun getPendingAppIds(): Set<String> {
        return prefs.getStringSet("pending_app_ids", emptySet()) ?: emptySet()
    }

    fun clearPendingAppIds() {
        prefs.edit() { remove("pending_app_ids") }
        _pendingAppIds.value = emptySet()
        Log.d("SyncHandler", "Cleared pending app IDs.")
    }

    // --- Métodos para pendientes de eliminación de Apps ---
    fun addDeletedAppId(packageName: String) {
        val current = getDeletedAppIds()
        val newSet = current.toMutableSet()
        newSet.add(packageName)
        prefs.edit() { putStringSet("deleted_app_ids", newSet) }
        _deletedAppIds.value = newSet
        Log.d("SyncHandler", "Added deleted app ID: $packageName.")
    }

    fun getDeletedAppIds(): Set<String> {
        return prefs.getStringSet("deleted_app_ids", emptySet()) ?: emptySet()
    }

    fun clearDeletedAppIds() {
        prefs.edit() { remove("deleted_app_ids") }
        _deletedAppIds.value = emptySet()
        Log.d("SyncHandler", "Cleared deleted app IDs.")
    }

    // --- Métodos para actualización de dispositivo ---
    fun markDeviceUpdatePending() {
        prefs.edit() { putBoolean("device_update_pending", true) }
        _deviceUpdatePending.value = true
        Log.d("SyncHandler", "Marked device update as pending.")
    }

    fun isDeviceUpdatePending(): Boolean {
        return prefs.getBoolean("device_update_pending", false)
    }

    fun clearDeviceUpdatePending() {
        prefs.edit() { remove("device_update_pending") }
        _deviceUpdatePending.value = false
        Log.d("SyncHandler", "Cleared device update pending flag.")
    }

}