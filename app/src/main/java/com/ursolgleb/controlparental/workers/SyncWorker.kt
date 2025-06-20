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

        Log.e("SyncWorker", "Ejecutando doWork() vercion 3...")

        val entryPoint = EntryPointAccessors
            .fromApplication(
                applicationContext,
                SyncWorkerEntryPoint::class.java
            )
        val localRepo = entryPoint.getAppDataRepository()
        val remoteRepo = entryPoint.getRemoteDataRepository()
        val syncHandler = entryPoint.getSyncHandler()
        val horarioDao = entryPoint.getHorarioDao()

        try {
            val device = localRepo.getDeviceInfoOnce()?.toDto()

            if (device != null) {
                remoteRepo.pushDevice(device)
                Log.w("SyncWorker", "pushDevice...")
            }

            // HORARIO--------------------
/*            if (syncHandler.isPushHorarioPendiente()) {
                Log.w("SyncWorker", "PUSH Horario...")
                val horarios = horarioDao.getAllHorariosOnce()
                Log.w("SyncWorker", "horarios: $horarios")
                if (horarios.isNotEmpty()) {
                    val horariosDto = horarios.map { it.toDto() }
                    Log.e("SyncWorker", "deviceId: ${device?.deviceId} horarios: $horariosDto")
                    remoteRepo.pushHorarios(horariosDto)
                } else {
                    //remoteRepo.deleteHorarios(listOf(device?.deviceId.toString()))
                    Log.w("SyncWorker", "delete Horarios...")
                }
                syncHandler.setPushHorarioPendiente(false)
            } else {
                Log.w("SyncWorker", "FETCH Horario...")
                val remoteHorarios = remoteRepo.fetchHorarios(device?.deviceId)
                Log.e("SyncWorker", "deviceId: ${device?.deviceId} horarios: $remoteHorarios")
                localRepo.deleteAllHorarios().await()
                if (remoteHorarios.isNotEmpty()) {
                    remoteHorarios.mapNotNull { it.toEntity() }.forEach { horario ->
                        localRepo.addHorarioBD(horario)
                    }
                }
            }*/


            /*            val remoteApps = remoteRepo.fetchApps(device?.deviceId)
                        if (remoteApps.isNotEmpty()) {
                            val entities = remoteApps.mapNotNull { it.toEntity() }
                            if (entities.isNotEmpty()) {
                                Log.e("SyncWorker", "Ejecutando doWork() insertAppsEntidades Start...")
                                localRepo.insertAppsEntidades(entities)
                                Log.e("SyncWorker", "Ejecutando doWork() insertAppsEntidades End...")
                            }
                        }*/

            localRepo.updateTiempoUsoAppsHoy().await()


            // PUSH------------------
            if (localRepo.todosAppsFlow.value.isNotEmpty()) {
                val apps = localRepo.todosAppsFlow.value.map { it.toDto() }
                remoteRepo.pushApps(apps)
            } else {
                remoteRepo.deleteApps(listOf(device?.deviceId.toString()))
            }


            //  Reprogramar el worker
            scheduleNextWork(applicationContext)
            Log.e("SyncWorker", "Ejecutando doWork() scheduleNextWork...")
            return Result.success()

        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()
            Log.e("SyncWorker", "HTTP error ${e.code()} body: $body")
            return Result.retry()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error ${e.message}")
            return Result.retry()
        } catch (e: EOFException) {
            Log.e("SyncWorker", "EOFException: ${e.message}", e)
            return Result.retry() // O Result.failure() según tu lógica
        }


    }

    private fun scheduleNextWork(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setInitialDelay(30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "SyncWorker",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            workRequest
        )
    }

    companion object {
        fun startWorker(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "SyncWorker",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest
            )
        }
    }

}
