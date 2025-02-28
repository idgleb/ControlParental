package com.ursolgleb.controlparental.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ursolgleb.controlparental.AppDataRepository
import com.ursolgleb.controlparental.di.AppUsageWorkerEntryPoint
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext


class AppUsageWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("AppUsageWorker", "Ejecutando updateTiempoUsoApps...")

        // ✅ Recuperar `AppDataRepository` manualmente usando `EntryPoints`
        val appDataRepository = EntryPointAccessors
            .fromApplication(applicationContext, AppUsageWorkerEntryPoint::class.java)
            .getAppDataRepository()


        // ✅ Llamar a updateTiempoUsoApps()
        appDataRepository.updateTiempoUsoApps()

        // 🔹 Reprogramar el worker después de 10 segundos
        scheduleNextWork(applicationContext)

        return Result.success()
    }

    private fun scheduleNextWork(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<AppUsageWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "AppUsageWorker",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            workRequest
        )
    }
}
