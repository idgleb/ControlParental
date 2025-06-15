package com.ursolgleb.controlparental

import android.app.Application
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.validadors.PinValidator
import com.ursolgleb.controlparental.workers.SyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import androidx.emoji2.text.EmojiCompat



@HiltAndroidApp
class ControlParentalApp : Application(), Configuration.Provider {
    // proba Gleb 12-06-2025

    @Inject lateinit var pinValidator: PinValidator

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
        EmojiCompat.init(BundledEmojiCompatConfig(this))

        appDataRepository.inicieDelecturaDeBD()
        appDataRepository.updateBDApps()

        //SyncWorker.startWorker(this)

        pinValidator.savePin("1234")   // ejecuta al confirmar el PIN

        appDataRepository.saveDeviceInfo()


    }

    override fun onTerminate() {
        super.onTerminate()
        appDataRepository.cancelarCorrutinas() // Cancela las corrutinas al cerrar la app
        coroutineScope.cancel()
    }

}
