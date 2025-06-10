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

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {

        Log.e("SyncWorker", "Ejecutando doWork() vercion 2...")

        val entryPoint = EntryPointAccessors
            .fromApplication(
                applicationContext,
                SyncWorkerEntryPoint::class.java
            )
        val localRepo = entryPoint.getAppDataRepository()
        val remoteRepo = entryPoint.getRemoteDataRepository()

        Log.e("SyncWorker", "Ejecutando doWork() entryPoint...")

        //localRepo.updateTiempoUsoAppsHoy()

        try {

            /* val apps = localRepo.todosAppsFlow.value.map { it.toDto() }
             remoteRepo.pushApps(apps)
             val horarios = localRepo.horariosFlow.value.map { it.toDto() }
             remoteRepo.pushHorarios(horarios)*/


            val remoteApps = remoteRepo.fetchApps()
            if (remoteApps.isNotEmpty()) {
                val icon = localRepo.todosAppsFlow.value.firstOrNull()?.appIcon
                    ?: return Result.success()
                val entities = remoteApps.mapNotNull { it.toEntity(icon) }
                if (entities.isNotEmpty()) {
                    Log.e("SyncWorker", "Ejecutando doWork() insertAppsEntidades Start...")
                    localRepo.insertAppsEntidades(entities)
                    Log.e("SyncWorker", "Ejecutando doWork() insertAppsEntidades End...")
                }
            }


            val remoteHorarios = remoteRepo.fetchHorarios()
            if (remoteHorarios.isNotEmpty()) {
                remoteHorarios.mapNotNull { it.toEntity() }.forEach { horario ->
                    localRepo.addHorarioBD(horario)
                }
            }

            //  Reprogramar el worker
            scheduleNextWork(applicationContext)
            Log.e("SyncWorker", "Ejecutando doWork() scheduleNextWork...")
            return Result.success()

        } catch (e: Exception) {
            Log.e("SyncWorker", "Error $e")
            return Result.retry()
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
