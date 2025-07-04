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
import com.ursolgleb.controlparental.di.ModernSyncWorkerEntryPoint
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
                ModernSyncWorkerEntryPoint::class.java
            )
        
        val localRepo = entryPoint.getAppDataRepository()
        val eventSyncManager = entryPoint.getEventSyncManager()
        Log.d(TAG, "Dependencies obtained successfully")

        return try {
            val deviceId = localRepo.getOrCreateDeviceId()
            
            // FLUJO IDEAL DE SINCRONIZACIÓN
            
            // Paso 1: Verificar si necesitamos sincronización completa
            val hasLocalHorarios = localRepo.horariosFlow.value.isNotEmpty()
            val hasLocalApps = localRepo.todosAppsFlow.value.isNotEmpty()
            val isFirstSync = !hasLocalHorarios || !hasLocalApps
            
            if (isFirstSync) {
                Log.d(TAG, "First sync detected, performing full sync...")
                
                // Sincronización completa para datos iniciales
                if (!hasLocalHorarios) {
                    Log.d(TAG, "Fetching all horarios...")
                    val horariosResult = localRepo.getHorarios(deviceId).first { it !is Resource.Loading }
                    when (horariosResult) {
                        is Resource.Success -> Log.d(TAG, "Horarios sync success: ${horariosResult.data?.size ?: 0} items")
                        is Resource.Error -> Log.e(TAG, "Horarios sync error: ${horariosResult.message}")
                        else -> Log.d(TAG, "Horarios sync completed")
                    }
                }
                
                if (!hasLocalApps) {
                    Log.d(TAG, "Fetching all apps...")
                    val appsResult = localRepo.getApps(deviceId).first { it !is Resource.Loading }
                    when (appsResult) {
                        is Resource.Success -> Log.d(TAG, "Apps sync success: ${appsResult.data?.size ?: 0} items")
                        is Resource.Error -> Log.e(TAG, "Apps sync error: ${appsResult.message}")
                        else -> Log.d(TAG, "Apps sync completed")
                    }
                }
            }
            
            // Paso 2: Sincronización incremental por eventos (siempre se ejecuta)
            Log.d(TAG, "Starting incremental event sync...")
            val syncEventsResult = eventSyncManager.sync()
            
            if (syncEventsResult.isSuccess) {
                Log.d(TAG, "Event sync finished successfully")
                
                // Paso 3: Si los eventos indican cambios, las próximas llamadas a getHorarios/getApps
                // los descargarán automáticamente gracias a hasPendingServerChanges()
                
            } else {
                Log.e(TAG, "Event sync failed: ${syncEventsResult.exceptionOrNull()?.message}")
                
                // En caso de error persistente, marcar para re-sincronización completa
                if (syncEventsResult.exceptionOrNull() is java.net.UnknownHostException) {
                    Log.d(TAG, "Network unavailable, will retry later")
                } else {
                    // Marcar que hay cambios pendientes para forzar sincronización en la próxima oportunidad
                    localRepo.markServerChanges("horario", true)
                    localRepo.markServerChanges("app", true)
                }
            }

            Log.d(TAG, "Modern sync cycle completed successfully.")
            scheduleNextWork(applicationContext)
            return Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Error in ModernSyncWorker", e)
            // Siempre reprogramar, incluso si hay error, para asegurar la resiliencia.
            scheduleNextWork(applicationContext)
            return Result.retry()
        }
    }

    private fun scheduleNextWork(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<ModernSyncWorker>()
            .setInitialDelay(4, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.APPEND,
            workRequest
        )
    }
} 