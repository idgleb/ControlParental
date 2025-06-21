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

        try {
            val device = localRepo.getDeviceInfoOnce()?.toDto()

            if (device != null) {
                remoteRepo.pushDevice(device)
                Log.w("SyncWorker", "pushDevice...$device")
            }

        /*    // HORARIO--------------------
            if (syncHandler.isPushHorarioPendiente()) {
                //PUSH HORARIO
                Log.w("SyncWorker", "PUSH Horario...")
                val horariosDto = localRepo.horariosFlow.first().map { it.toDto() }
                if (horariosDto.isNotEmpty()) {
                    remoteRepo.pushHorarios(horariosDto)
                } else {
                    remoteRepo.deleteHorarios(listOf(device?.deviceId.toString()))
                }
                syncHandler.setPushHorarioPendiente(false)
            } else {
                //FETCH HORARIO
                Log.w("SyncWorker", "FETCH Horario...")
                val remoteHorarios = remoteRepo.fetchHorarios(device?.deviceId)
                localRepo.deleteAllHorarios().await()
                if (remoteHorarios.isNotEmpty()) {
                    val horariosEntity = remoteHorarios.mapNotNull { it.toEntity() }
                    if (horariosEntity.isNotEmpty()) {
                        localRepo.insertHorariosEntidades(horariosEntity)
                    }
                }
            }*/

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
                val remoteApps = remoteRepo.fetchApps(device?.deviceId)
                Log.e("SyncWorker", "remoteApps: $remoteApps")
                localRepo.deleteAllApps().await()
                if (remoteApps.isNotEmpty()) {
                    val appsEntity = remoteApps.mapNotNull { it.toEntity() }
                    if (appsEntity.isNotEmpty()) {
                        localRepo.insertAppsEntidades(appsEntity)
                    }
                }
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
            .setInitialDelay(15, TimeUnit.SECONDS)
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
