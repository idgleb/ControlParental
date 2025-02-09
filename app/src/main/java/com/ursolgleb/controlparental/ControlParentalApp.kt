package com.ursolgleb.controlparental

import android.app.Application
import com.ursolgleb.controlparental.data.local.AppDatabase
import com.ursolgleb.controlparental.data.log.AppBlockerDatabase

class ControlParentalApp: Application()  {
    companion object{
        lateinit var db: AppBlockerDatabase
            private set
        lateinit var db2: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        db = AppBlockerDatabase.getDatabase(this)
        db2 = AppDatabase.getDatabase(this)

    }
}