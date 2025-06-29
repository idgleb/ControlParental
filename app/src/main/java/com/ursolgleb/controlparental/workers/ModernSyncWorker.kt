package com.ursolgleb.controlparental.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ursolgleb.controlparental.data.remote.models.toDto
import com.ursolgleb.controlparental.di.SyncWorkerEntryPoint
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit
import com.ursolgleb.controlparental.data.remote.models.Resource

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
        // val remoteRepo = entryPoint.getRemoteDataRepository() // Ya no se usa aquí
        // val eventSyncManager = entryPoint.getEventSyncManager() // Ya no se usa aquí
        // val syncHandler = entryPoint.getSyncHandler() // Ya no se usa aquí

        Log.d(TAG, "Dependencies obtained successfully")

        return try {
            val deviceId = localRepo.getOrCreateDeviceId()
            
            // Disparar la sincronización de horarios.
            // La lógica de si debe o no ir a la red está dentro del NetworkBoundResource.
            Log.d(TAG, "Triggering Horarios sync...")
            localRepo.getHorarios(deviceId).collect { resource ->
                // Opcional: loguear el estado del recurso
                when (resource) {
                    is Resource.Success -> Log.d(TAG, "Horarios sync success: ${resource.data?.size} items")
                    is Resource.Error -> Log.e(TAG, "Horarios sync error: ${resource.message}")
                    is Resource.Loading -> Log.d(TAG, "Horarios sync loading...")
                }
            }
            
            // (Aquí se añadiría la llamada a getApps cuando se implemente)

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
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }
} 