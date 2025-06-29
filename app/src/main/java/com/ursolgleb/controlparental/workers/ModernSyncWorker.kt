package com.ursolgleb.controlparental.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ursolgleb.controlparental.data.common.Resource
import com.ursolgleb.controlparental.data.remote.models.toDto
import com.ursolgleb.controlparental.di.SyncWorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Worker moderno que usa sincronización basada en eventos
 */
class ModernSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "ModernSyncWorker"
        private const val WORK_NAME = "ModernSyncWorker"
        
        fun startWorker(context: Context) {
            Log.d(TAG, "startWorker called")
            val workRequest = OneTimeWorkRequestBuilder<ModernSyncWorker>()
                .build()
            
            Log.d(TAG, "Enqueuing work request with name: $WORK_NAME")
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                workRequest
            )
            Log.d(TAG, "Work request enqueued successfully")
        }
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "ModernSyncWorker iniciado - ${System.currentTimeMillis()}")
        Log.d(TAG, "Starting modern sync...")

        val entryPoint = EntryPointAccessors
            .fromApplication(
                applicationContext,
                SyncWorkerEntryPoint::class.java
            )
        
        val localRepo = entryPoint.getAppDataRepository()
        val eventSyncManager = entryPoint.getEventSyncManager()

        Log.d(TAG, "Dependencies obtained successfully")

        return try {
            val deviceId = localRepo.getOrCreateDeviceId()
            
            // Disparar la sincronización de horarios.
            // La lógica de si debe o no ir a la red está dentro del NetworkBoundResource.
            Log.d(TAG, "Triggering Horarios sync...")

            // Obtener solo el primer resultado no-loading para evitar que el
            // worker se quede recolectando actualizaciones indefinidamente
            val syncResult = localRepo
                .getHorarios(deviceId)
                .first { it !is Resource.Loading }

            when (syncResult) {
                is Resource.Success ->
                    Log.d(TAG, "Horarios sync success: ${syncResult.data?.size} items")
                is Resource.Error ->
                    Log.e(TAG, "Horarios sync error: ${syncResult.message}")
                else -> {
                    // No debería emitirse otro tipo aquí, pero se ignora por seguridad
                    Log.d(TAG, "Horarios sync finished with state: ${syncResult::class.simpleName}")
                }
            }
            
            // (Aquí se añadiría la llamada a getApps cuando se implemente)
            // Asegurar que los cambios locales se envíen al servidor
            val syncEventsResult = eventSyncManager.sync()
            if (syncEventsResult.isSuccess) {
                Log.d(TAG, "Event sync finished successfully")
            } else {
                Log.e(TAG, "Event sync failed: ${syncEventsResult.exceptionOrNull()?.message}")
            }


            Log.d(TAG, "Modern sync cycle completed successfully.")
            scheduleNextWork(applicationContext)
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Error in ModernSyncWorker", e)
            Result.retry() // Reintentar si hay un error inesperado
        }
    }

    private fun scheduleNextWork(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<ModernSyncWorker>()
            .setInitialDelay(15, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.APPEND,
            workRequest
        )
    }
} 