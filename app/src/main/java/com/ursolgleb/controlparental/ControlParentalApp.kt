package com.ursolgleb.controlparental

import android.app.Application
import android.content.Context
import com.ursolgleb.controlparental.data.log.LogAppBlockerDatabase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ursolgleb.controlparental.workers.AppUsageWorker


@HiltAndroidApp
class ControlParentalApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appDataRepository: AppDataRepository


    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        lateinit var dbLogs: LogAppBlockerDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        dbLogs = LogAppBlockerDatabase.getDatabase(this)

        appDataRepository.inicieDelecturaDeBD()
        appDataRepository.updateBDApps()

        startWorker(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        appDataRepository.clear() // ✅ Cancela las corrutinas al cerrar la app
    }

    fun startWorker(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<AppUsageWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "AppUsageWorker",
            ExistingWorkPolicy.APPEND_OR_REPLACE, // 🔹 NO cancela el anterior, lo agrega en cola
            workRequest
        )
    }



}
