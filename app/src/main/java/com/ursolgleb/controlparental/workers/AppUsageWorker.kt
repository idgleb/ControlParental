package com.ursolgleb.controlparental.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ursolgleb.controlparental.di.AppUsageWorkerEntryPoint
import java.util.concurrent.TimeUnit
import dagger.hilt.android.EntryPointAccessors


class AppUsageWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.e("MioParametro", "Ejecutando doWork()...")

        //  Recuperar `AppDataRepository` manualmente usando `EntryPoints`
        val appDataRepository = EntryPointAccessors
            .fromApplication(applicationContext, AppUsageWorkerEntryPoint::class.java)
            .getAppDataRepository()

        Log.w("MioParametro", "Worker blockedAppsFlow: ${appDataRepository.blockedAppsFlow.value.map { it.appName }}")

        appDataRepository.updateTiempoUsoAppsHoy()

        //  Reprogramar el worker
        scheduleNextWork(applicationContext)

        return Result.success()
    }

    private fun scheduleNextWork(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<AppUsageWorker>()
            .setInitialDelay(60, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "AppUsageWorker",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            workRequest
        )
    }
}
