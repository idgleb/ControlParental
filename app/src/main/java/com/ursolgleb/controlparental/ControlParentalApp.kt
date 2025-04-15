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
import com.ursolgleb.controlparental.UI.activities.DesarolloActivity
import com.ursolgleb.controlparental.utils.Archivo
import com.ursolgleb.controlparental.workers.AppUsageWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


@HiltAndroidApp
class ControlParentalApp : Application(), Configuration.Provider {
    // proba Gleb

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appDataRepository: AppDataRepository

    private val coroutineScope = CoroutineScope(Dispatchers.IO)


    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()


    override fun onCreate() {
        super.onCreate()
        appDataRepository.inicieDelecturaDeBD()
        appDataRepository.updateBDApps()
        //
        //startWorker(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        appDataRepository.cancelarCorrutinas() // ✅ Cancela las corrutinas al cerrar la app
        coroutineScope.cancel()
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
