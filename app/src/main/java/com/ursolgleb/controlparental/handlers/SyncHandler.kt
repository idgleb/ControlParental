package com.ursolgleb.controlparental.handlers

import android.content.Context
import android.util.Log
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.data.apps.dao.SyncDataDao
import com.ursolgleb.controlparental.data.apps.entities.SyncDataEntity
import com.ursolgleb.controlparental.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import javax.inject.Provider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDataRepository: Provider<AppDataRepository>,
    val syncDataDao: SyncDataDao,
) {

    private val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    // --- Métodos para pendientes de creación/actualización de Horarios ---
    fun addPendingHorarioId(id: Long) {
        val current = getPendingHorarioIds()
        val newSet = current.toMutableSet()
        newSet.add(id.toString())
        prefs.edit().putStringSet("pending_horario_ids", newSet).apply()
        Log.d("SyncHandler", "Added pending horario ID: $id.")
    }

    fun getPendingHorarioIds(): Set<String> {
        return prefs.getStringSet("pending_horario_ids", emptySet()) ?: emptySet()
    }

    fun clearPendingHorarioIds() {
        prefs.edit().remove("pending_horario_ids").apply()
        Log.d("SyncHandler", "Cleared pending horario IDs.")
    }

    // --- Métodos para pendientes de eliminación de Horarios ---
    fun addDeletedHorarioId(id: Long) {
        val current = getDeletedHorarioIds()
        val newSet = current.toMutableSet()
        newSet.add(id.toString())
        prefs.edit().putStringSet("deleted_horario_ids", newSet).apply()
        Log.d("SyncHandler", "Added deleted horario ID: $id.")
    }

    fun getDeletedHorarioIds(): Set<String> {
        return prefs.getStringSet("deleted_horario_ids", emptySet()) ?: emptySet()
    }

    fun clearDeletedHorarioIds() {
        prefs.edit().remove("deleted_horario_ids").apply()
        Log.d("SyncHandler", "Cleared deleted horario IDs.")
    }

    // --- Métodos para pendientes de creación/actualización de Apps ---
    fun addPendingAppId(packageName: String) {
        val current = getPendingAppIds()
        val newSet = current.toMutableSet()
        newSet.add(packageName)
        prefs.edit().putStringSet("pending_app_ids", newSet).apply()
        Log.d("SyncHandler", "Added pending app ID: $packageName.")
    }

    fun getPendingAppIds(): Set<String> {
        return prefs.getStringSet("pending_app_ids", emptySet()) ?: emptySet()
    }

    fun clearPendingAppIds() {
        prefs.edit().remove("pending_app_ids").apply()
        Log.d("SyncHandler", "Cleared pending app IDs.")
    }

    // --- Métodos para pendientes de eliminación de Apps ---
    fun addDeletedAppId(packageName: String) {
        val current = getDeletedAppIds()
        val newSet = current.toMutableSet()
        newSet.add(packageName)
        prefs.edit().putStringSet("deleted_app_ids", newSet).apply()
        Log.d("SyncHandler", "Added deleted app ID: $packageName.")
    }

    fun getDeletedAppIds(): Set<String> {
        return prefs.getStringSet("deleted_app_ids", emptySet()) ?: emptySet()
    }

    fun clearDeletedAppIds() {
        prefs.edit().remove("deleted_app_ids").apply()
        Log.d("SyncHandler", "Cleared deleted app IDs.")
    }

    suspend fun getSyncDataInfoOnce(): SyncDataEntity? {
        return try {
            syncDataDao.getSyncDataOnce()
        } catch (e: Exception) {
            Logger.error(
                context,
                "SyncHandler",
                "Error obteniendo SyncData info: ${e.message}",
                e
            )
            null
        }
    }

}