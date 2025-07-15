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
        val authLocalDataSource = entryPoint.getDeviceAuthLocalDataSource()
        val remoteRepo = entryPoint.getRemoteDataRepository()
        val syncHandler = entryPoint.getSyncHandler()
        Log.d(TAG, "Dependencies obtained successfully")

        // Verificar si tenemos token de autenticación
        val token = authLocalDataSource.getApiToken()
        if (token == null) {
            Log.w(TAG, "No hay token de autenticación. El dispositivo debe ser verificado primero.")
            // NO reprogramar el worker si no hay autenticación
            return Result.success()
        }

        // Heartbeat: enviar datos básicos al backend
        try {
            sendHeartbeat(localRepo, remoteRepo, syncHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando heartbeat", e)
        }

        return try {
            val deviceId = try {
                localRepo.getOrCreateDeviceId()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "No hay deviceId de autenticación disponible.")
                // No reprogramar si no hay deviceId
                return Result.success()
            }

            // FLUJO DE SINCRONIZACIÓN
            
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
                        is Resource.Error -> {
                            Log.e(TAG, "Horarios sync error: ${horariosResult.message}")
                            // Si es error 401, no reprogramar
                            if (horariosResult.message?.contains("401") == true) {
                                Log.w(TAG, "Error de autenticación, no reprogramando worker")
                                return Result.success()
                            }
                        }
                        else -> Log.d(TAG, "Horarios sync completed")
                    }
                }
                
                if (!hasLocalApps) {
                    Log.d(TAG, "Fetching all apps...")
                    val appsResult = localRepo.getApps(deviceId).first { it !is Resource.Loading }
                    when (appsResult) {
                        is Resource.Success -> Log.d(TAG, "Apps sync success: ${appsResult.data?.size ?: 0} items")
                        is Resource.Error -> {
                            Log.e(TAG, "Apps sync error: ${appsResult.message}")
                            // Si es error 401, no reprogramar
                            if (appsResult.message?.contains("401") == true) {
                                Log.w(TAG, "Error de autenticación, no reprogramando worker")
                                return Result.success()
                            }
                        }
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
                
                // Si es error de autenticación, no reprogramar
                val error = syncEventsResult.exceptionOrNull()
                if (error is retrofit2.HttpException && error.code() == 401) {
                    Log.w(TAG, "Error de autenticación 401, no reprogramando worker")
                    return Result.success()
                }
                
                // En caso de error persistente, marcar para re-sincronización completa
                if (error is java.net.UnknownHostException) {
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
            
            // Si es error de autenticación, no reprogramar
            if (e is retrofit2.HttpException && e.code() == 401) {
                Log.w(TAG, "Error de autenticación 401, no reprogramando worker")
                return Result.success()
            }
            
            // Para otros errores, reprogramar
            scheduleNextWork(applicationContext)
            return Result.retry()
        }
    }

    private fun scheduleNextWork(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<ModernSyncWorker>()
            .setInitialDelay(5, TimeUnit.SECONDS) // Aumentado a 30 segundos
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.APPEND,
            workRequest
        )
    }

    private suspend fun sendHeartbeat(
        localRepo: com.ursolgleb.controlparental.data.local.AppDataRepository,
        remoteRepo: com.ursolgleb.controlparental.data.remote.RemoteDataRepository,
        syncHandler: com.ursolgleb.controlparental.handlers.SyncHandler
    ) {
        try {
            val device = localRepo.getDeviceInfoOnce() ?: run {
                Log.e("ModernSyncWorker", "No device info available")
                return
            }
            val bm = applicationContext.getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
            val currentBattery = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            val currentModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

            // Verificar si hay cambios en el dispositivo
            var hasDeviceChanges = false
            if (device.model != currentModel || device.batteryLevel != currentBattery) {
                hasDeviceChanges = true
            }

            // Enviar heartbeat al servidor (sin ubicación)
            val response = remoteRepo.sendHeartbeat(
                deviceId = device.deviceId,
                latitude = null,
                longitude = null
            )

            if (response.isSuccessful) {
                Log.d("ModernSyncWorker", "Heartbeat sent successfully (sin ubicación)")
                val updatedDevice = device.copy(
                    model = currentModel,
                    batteryLevel = currentBattery,
                    lastSeen = System.currentTimeMillis(),
                    pingIntervalSeconds = 4 // igual que HeartbeatService
                )
                localRepo.updateDeviceInfo(updatedDevice)
                if (hasDeviceChanges) {
                    syncHandler.markDeviceUpdatePending()
                    Log.w("ModernSyncWorker", "Device info changed (battery/model), marked for sync")
                }
            } else {
                Log.e("ModernSyncWorker", "Heartbeat failed: ${response.code()}")
                if (response.code() == 401 || response.code() == 403) {
                    throw IllegalStateException("Authentication error: ${response.code()}")
                }
            }
        } catch (e: IllegalStateException) {
            throw e
        } catch (e: Exception) {
            Log.e("ModernSyncWorker", "Error sending heartbeat", e)
        }
    }
} 