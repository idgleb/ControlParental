package com.ursolgleb.controlparental.workers

import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


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

        appDataRepository.updateTiempoUsoAppsHoy()



        if (appBlockHandler.isBlocking) {
            if (appDataRepository.currentPkg != appDataRepository.defLauncher) {
                appBlockHandler.log("🔴 Ejecutando GLOBAL_ACTION_HOME (x2)", appDataRepository.currentPkg!!)
                performGlobalAction(GLOBAL_ACTION_HOME)
                coroutineScope.launch {
                    delay(500)
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            } else {
                appBlockHandler.log("🟢 En launcher, reseteando bloqueo", appDataRepository.currentPkg!!)
                performGlobalAction(GLOBAL_ACTION_HOME)
                appBlockHandler.resetBlockFlag()
            }
        }



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
