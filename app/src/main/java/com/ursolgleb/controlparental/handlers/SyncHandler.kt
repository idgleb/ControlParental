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

    suspend fun isPushHorarioPendiente(): Boolean {
        return try {
            val result = getSyncDataInfoOnce()?.isPushHorarioPendiente ?: false
            Log.d("SyncHandler", "isPushHorarioPendiente: $result")
            result
        } catch (e: Exception) {
            // Usar SharedPreferences como respaldo
            val result = prefs.getBoolean("push_horario_pendiente", false)
            Log.d("SyncHandler", "isPushHorarioPendiente from prefs: $result")
            result
        }
    }

    suspend fun isPushAppsPendiente(): Boolean {
        return try {
            val result = getSyncDataInfoOnce()?.isPushAppsPendiente ?: false
            Log.d("SyncHandler", "isPushAppsPendiente: $result")
            result
        } catch (e: Exception) {
            // Usar SharedPreferences como respaldo
            val result = prefs.getBoolean("push_apps_pendiente", false)
            Log.d("SyncHandler", "isPushAppsPendiente from prefs: $result")
            result
        }
    }

    fun setPushHorarioPendiente(pendiente: Boolean): Deferred<Unit> = appDataRepository.get().scope.async {
        Log.d("SyncHandler", "setPushHorarioPendiente called with: $pendiente")
        try {
            val syncData = getSyncDataInfoOnce() ?: SyncDataEntity((appDataRepository.get().getOrCreateDeviceId()))
            syncData.isPushHorarioPendiente = pendiente
            syncDataDao.insert(syncData)
            Log.d("SyncHandler", "setPushHorarioPendiente saved: $pendiente")
        } catch (e: Exception) {
            Log.e("SyncHandler", "Error saving to DB, using prefs", e)
        }
        // Siempre guardar en SharedPreferences como respaldo
        prefs.edit().putBoolean("push_horario_pendiente", pendiente).apply()
    }

    fun setPushAppsPendiente(pendiente: Boolean): Deferred<Unit> = appDataRepository.get().scope.async {
        Log.d("SyncHandler", "setPushAppsPendiente called with: $pendiente")
        try {
            val syncData = getSyncDataInfoOnce() ?: SyncDataEntity((appDataRepository.get().getOrCreateDeviceId()))
            syncData.isPushAppsPendiente = pendiente
            syncDataDao.insert(syncData)
            Log.d("SyncHandler", "setPushAppsPendiente saved: $pendiente")
        } catch (e: Exception) {
            Log.e("SyncHandler", "Error saving to DB, using prefs", e)
        }
        // Siempre guardar en SharedPreferences como respaldo
        prefs.edit().putBoolean("push_apps_pendiente", pendiente).apply()
    }

    fun getLastHorarioSync(): String? {
        return prefs.getString("last_horario_sync", null)
    }

    fun setLastHorarioSync(timestamp: String?) {
        prefs.edit().putString("last_horario_sync", timestamp).apply()
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