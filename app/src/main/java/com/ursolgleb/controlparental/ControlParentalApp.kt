package com.ursolgleb.controlparental

import android.app.Application
import com.ursolgleb.controlparental.data.local.AppDatabase
import com.ursolgleb.controlparental.data.log.LogAppBlockerDatabase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ControlParentalApp: Application()  {

    companion object{
        lateinit var dbLogs: LogAppBlockerDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        dbLogs = LogAppBlockerDatabase.getDatabase(this)

    }
}