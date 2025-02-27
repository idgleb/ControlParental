package com.ursolgleb.controlparental

import android.app.Application
import com.ursolgleb.controlparental.data.log.LogAppBlockerDatabase
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ControlParentalApp: Application()  {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    companion object{
        lateinit var dbLogs: LogAppBlockerDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        dbLogs = LogAppBlockerDatabase.getDatabase(this)
        appDataRepository.inicieDelecturaDeBD()
        appDataRepository.updateBDApps()

    }

    override fun onTerminate() {
        super.onTerminate()
        appDataRepository.clear() // ✅ Cancela las corrutinas al cerrar la app
    }





}