package com.ursolgleb.controlparental.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ursolgleb.controlparental.data.remote.models.toDto
import com.ursolgleb.controlparental.data.remote.models.toEntity
import com.ursolgleb.controlparental.di.SyncWorkerEntryPoint
import java.util.concurrent.TimeUnit
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.io.EOFException

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.e("SyncWorker", "\n")
        Log.e("SyncWorker", "\nEjecutando doWork() vercion 3...")

        val entryPoint = EntryPointAccessors
            .fromApplication(
                applicationContext,
                SyncWorkerEntryPoint::class.java
            )
        val localRepo = entryPoint.getAppDataRepository()
        val remoteRepo = entryPoint.getRemoteDataRepository()
        val syncHandler = entryPoint.getSyncHandler()

        try {

            val device = localRepo.getDeviceInfoOnce()?.toDto()

            // DEVICE--------------------
            if (device != null) {
                remoteRepo.pushDevice(device)
                Log.w("SyncWorker", "pushDevice...$device")
            } else return Result.success()

            // HORARIO--------------------
            if (syncHandler.isPushHorarioPendiente()) {
                //PUSH HORARIO
                Log.w("SyncWorker", "PUSH Horario...")
                val horariosDto = localRepo.horariosFlow.first().map { it.toDto() }
                if (horariosDto.isNotEmpty()) {
                    remoteRepo.pushHorarios(horariosDto)
                } else {
                    remoteRepo.deleteHorarios(listOf(device.deviceId.toString()))
                }
                syncHandler.setPushHorarioPendiente(false)
            } else {
                //FETCH HORARIO
                Log.w("SyncWorker", "FETCH Horario...")
                
                // Obtener el timestamp de la última sincronización
                val lastSync = syncHandler.getLastHorarioSync()
                
                // Obtener los IDs de horarios actuales para detectar eliminaciones
                val currentHorarios = localRepo.horariosFlow.first()
                val knownIds: List<Long> = currentHorarios.map { it.idHorario }
                
                try {
                    // Llamar a fetchHorarios con los nuevos parámetros
                    val syncResponse = remoteRepo.fetchHorarios(
                        deviceId = device?.deviceId,
                        lastSync = lastSync,
                        knownIds = knownIds
                    )
                    
                    // Verificar si hay cambios
                    if (syncResponse.status == "no_changes") {
                        Log.d("SyncWorker", "No hay cambios en horarios")
                    } else if (syncResponse.status == "success") {
                        // Procesar cambios
                        val changes = syncResponse.changes
                        
                        // 1. Eliminar horarios que fueron borrados en el servidor
                        if (changes?.deleted?.isNotEmpty() == true) {
                            Log.d("SyncWorker", "Eliminando horarios: ${changes.deleted}")
                            changes.deleted.forEach { idHorario ->
                                localRepo.deleteHorarioByIdHorario(idHorario,
                                    device.deviceId.toString()
                                ).await()
                            }
                        }
                        
                        // 2. Insertar o actualizar horarios nuevos/modificados
                        val horariosToUpdate = syncResponse.data
                        if (!horariosToUpdate.isNullOrEmpty()) {
                            Log.d("SyncWorker", "Actualizando ${horariosToUpdate.size} horarios")
                            val horariosEntity = horariosToUpdate.mapNotNull { it.toEntity() }
                            if (horariosEntity.isNotEmpty()) {
                                localRepo.insertHorariosEntidades(horariosEntity)
                            }
                        }
                        
                        // 3. Guardar el timestamp de esta sincronización
                        syncResponse.timestamp?.let { timestamp ->
                            syncHandler.setLastHorarioSync(timestamp)
                        }
                        
                        Log.d("SyncWorker", "Sincronización de horarios completada - " +
                                "Agregados: ${changes?.added?.size ?: 0}, " +
                                "Actualizados: ${changes?.updated?.size ?: 0}, " +
                                "Eliminados: ${changes?.deleted?.size ?: 0}")
                    }
                } catch (e: Exception) {
                    // Si falla, hacer sincronización completa
                    Log.e("SyncWorker", "Error en sincronización incremental, haciendo sync completo: ${e.message}")
                    try {
                        // Usar el método legacy que devuelve List<HorarioDto>
                        val remoteHorarios = remoteRepo.fetchHorarios(device?.deviceId)
                        localRepo.deleteAllHorarios().await()
                        if (remoteHorarios.isNotEmpty()) {
                            val horariosEntity = remoteHorarios.mapNotNull { it.toEntity() }
                            if (horariosEntity.isNotEmpty()) {
                                localRepo.insertHorariosEntidades(horariosEntity)
                            }
                        }
                        // Resetear el timestamp para la próxima vez
                        syncHandler.setLastHorarioSync(null)
                    } catch (fallbackError: Exception) {
                        Log.e("SyncWorker", "Error en sincronización completa de horarios: ${fallbackError.message}")
                    }
                }
            }

            localRepo.updateTiempoUsoAppsHoy().await()

            // APPS------------------
            if (syncHandler.isPushAppsPendiente()) {
                //PUSH APPS
                Log.w("SyncWorker", "PUSH Apps...")
                val appsDto = localRepo.todosAppsFlow.value.map { it.toDto() }
                if (appsDto.isNotEmpty()) {
                    remoteRepo.pushApps(appsDto)
                } else {
                    remoteRepo.deleteApps(listOf(device?.deviceId.toString()))
                }
                syncHandler.setPushAppsPendiente(false)
            } else {
                //FETCH APPS
                Log.w("SyncWorker", "FETCH Apps...")
                try {
                    val remoteApps = remoteRepo.fetchApps(device?.deviceId, includeIcons = true)
                    Log.d("SyncWorker", "Successfully fetched ${remoteApps.size} apps")
                    
                    localRepo.deleteAllApps().await()
                    if (remoteApps.isNotEmpty()) {
                        val appsEntity = remoteApps.mapNotNull { it.toEntity() }
                        if (appsEntity.isNotEmpty()) {
                            localRepo.insertAppsEntidades(appsEntity)
                            Log.d("SyncWorker", "Inserted ${appsEntity.size} apps into local database")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Error fetching apps: ${e.message}", e)
                }
            }


            return Result.success()

        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()
            Log.e("SyncWorker", "HTTP error ${e.code()} body: $body")
            return Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error ${e.message}")
            return Result.success()
        } catch (e: EOFException) {
            Log.e("SyncWorker", "EOFException: ${e.message}", e)
            return Result.success()
        } finally {
            //  Reprogramar el worker
            scheduleNextWork(applicationContext)
            Log.e("SyncWorker", "Ejecutando doWork() scheduleNextWork...")
        }


    }

    private fun scheduleNextWork(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncWorker",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    companion object {
        fun startWorker(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "SyncWorker",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

}
