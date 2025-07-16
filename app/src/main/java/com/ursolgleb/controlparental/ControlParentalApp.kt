package com.ursolgleb.controlparental

import android.app.Application
import android.util.Log
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ursolgleb.controlparental.data.local.AppDataRepository
import com.ursolgleb.controlparental.validadors.PinValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import androidx.emoji2.text.EmojiCompat
import com.ursolgleb.controlparental.workers.ModernSyncWorker
import androidx.work.WorkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ursolgleb.controlparental.data.auth.local.DeviceAuthLocalDataSource


@HiltAndroidApp
class ControlParentalApp : Application(), Configuration.Provider {
    // proba Hijo Gleb 26-06-2025

    @Inject
    lateinit var pinValidator: PinValidator

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appDataRepository: AppDataRepository

    @Inject
    lateinit var deviceAuthLocalDataSource: DeviceAuthLocalDataSource

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .build()


    override fun onCreate() {
        super.onCreate()
        EmojiCompat.init(BundledEmojiCompatConfig(this))

        appDataRepository.inicieDelecturaDeBD()
        appDataRepository.updateBDApps()

        pinValidator.savePin("1234")   // ejecuta al confirmar el PIN

    }


    override fun onTerminate() {
        super.onTerminate()
        appDataRepository.cancelarCorrutinas() // Cancela las corrutinas al cerrar la app
        coroutineScope.cancel()

    }

}
