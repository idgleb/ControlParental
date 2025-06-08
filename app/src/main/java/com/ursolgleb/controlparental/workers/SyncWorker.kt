package com.ursolgleb.controlparental.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ursolgleb.controlparental.di.SyncWorkerEntryPoint
import com.ursolgleb.controlparental.data.remote.models.toDto
import com.ursolgleb.controlparental.data.remote.models.toEntity
import dagger.hilt.android.EntryPointAccessors
import java.util.concurrent.TimeUnit

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java
        )
        val localRepo = entryPoint.getAppDataRepository()
        val remoteRepo = entryPoint.getRemoteDataRepository()

        return try {
            val apps = localRepo.todosAppsFlow.value.map { it.toDto() }
            remoteRepo.pushApps(apps)

            val horarios = localRepo.horariosFlow.value.map { it.toDto() }
            remoteRepo.pushHorarios(horarios)

            val remoteApps = remoteRepo.fetchApps()
            if (remoteApps.isNotEmpty()) {
                val icon = localRepo.todosAppsFlow.value.firstOrNull()?.appIcon
                    ?: return Result.success()
                val entities = remoteApps.map { it.toEntity(icon) }
                localRepo.insertAppsEntidades(entities)
            }

            val remoteHorarios = remoteRepo.fetchHorarios()
            if (remoteHorarios.isNotEmpty()) {
                remoteHorarios.map { it.toEntity() }.forEach { horario ->
                    localRepo.addHorarioBD(horario)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "SyncWorker",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}