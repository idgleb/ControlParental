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
import com.ursolgleb.controlparental.services.HeartbeatService


@HiltAndroidApp
class ControlParentalApp : Application(), Configuration.Provider {
    // proba Hijo Gleb 26-06-2025

    @Inject lateinit var pinValidator: PinValidator

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var appDataRepository: AppDataRepository

    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var workerStarted = false // Flag para evitar iniciar múltiples veces

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()


    override fun onCreate() {
        super.onCreate()
        EmojiCompat.init(BundledEmojiCompatConfig(this))

        appDataRepository.inicieDelecturaDeBD()
        appDataRepository.updateBDApps()

        pinValidator.savePin("1234")   // ejecuta al confirmar el PIN

        appDataRepository.saveDeviceInfo().invokeOnCompletion {
            Log.e("ControlParentalApp", "saveDeviceInfo() completado")

            if (!workerStarted) {
                workerStarted = true
                
                // Cancelar todos los workers existentes para evitar duplicación
                Log.d("ControlParentalApp", "Cancelando todos los workers existentes...")
                WorkManager.getInstance(this).cancelAllWork()
                
                // Usar solo el sistema moderno basado en eventos
                Log.d("ControlParentalApp", "Iniciando ModernSyncWorker...")
                ModernSyncWorker.startWorker(this)
                Log.d("ControlParentalApp", "ModernSyncWorker programado")
                
                // Iniciar el servicio de heartbeat
                Log.d("ControlParentalApp", "Iniciando HeartbeatService...")
                HeartbeatService.start(this)
                
                // Verificar el estado del worker después de un breve delay
                coroutineScope.launch {
                    delay(1000)
                    checkWorkersStatus()
                }
            } else {
                Log.d("ControlParentalApp", "Worker ya iniciado, ignorando llamada duplicada")
            }
        }


    }

    private fun checkWorkersStatus() {
        val workManager = WorkManager.getInstance(this)
        workManager.getWorkInfosForUniqueWork("ModernSyncWorker")
            .get()
            .forEach { workInfo ->
                Log.d("ControlParentalApp", "ModernSyncWorker status: ${workInfo.state}, id: ${workInfo.id}")
            }
    }

    override fun onTerminate() {
        super.onTerminate()
        appDataRepository.cancelarCorrutinas() // Cancela las corrutinas al cerrar la app
        coroutineScope.cancel()
        
        // Detener el servicio de heartbeat
        HeartbeatService.stop(this)
    }

}
