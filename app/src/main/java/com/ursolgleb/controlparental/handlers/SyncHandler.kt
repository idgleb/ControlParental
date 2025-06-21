package com.ursolgleb.controlparental.handlers

import android.content.Context
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.data.apps.dao.AppDao
import com.ursolgleb.controlparental.data.apps.dao.SyncDataDao
import com.ursolgleb.controlparental.data.apps.entities.DeviceEntity
import com.ursolgleb.controlparental.data.apps.entities.SyncDataEntity
import com.ursolgleb.controlparental.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    val appDataRepository: AppDataRepository,
    private val syncDataDao: SyncDataDao,
) {


    suspend fun isPushHorarioPendiente(): Boolean {
        return getSyncDataInfoOnce()?.isPushHorarioPendiente == true
    }

    suspend fun isPushAppsPendiente(): Boolean {
        return getSyncDataInfoOnce()?.isPushAppsPendiente == true
    }

    fun setPushHorarioPendiente(pendiente: Boolean): Deferred<Unit> = appDataRepository.scope.async {
        val syncData = getSyncDataInfoOnce() ?: SyncDataEntity((appDataRepository.getOrCreateDeviceId()))
        syncData.isPushHorarioPendiente = pendiente
        syncDataDao.insert(syncData)
    }

    fun setPushAppsPendiente(pendiente: Boolean): Deferred<Unit> = appDataRepository.scope.async {
        val syncData = getSyncDataInfoOnce() ?: SyncDataEntity((appDataRepository.getOrCreateDeviceId()))
        syncData.isPushAppsPendiente = pendiente
        syncDataDao.insert(syncData)
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