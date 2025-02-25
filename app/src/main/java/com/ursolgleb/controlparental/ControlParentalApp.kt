package com.ursolgleb.controlparental

import android.app.Application
import android.os.DeadObjectException
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.ursolgleb.controlparental.data.local.AppDatabase
import com.ursolgleb.controlparental.data.log.LogAppBlockerDatabase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        appDataRepository.cargarAppsEnBackgroundDesdeBD()

    }





}