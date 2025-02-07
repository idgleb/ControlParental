package com.ursolgleb.controlparental

import android.app.Application
import com.ursolgleb.controlparental.UI.MainActivity
import com.ursolgleb.controlparental.data.AppBlockerDatabase

class ControlParentalApp: Application()  {
    companion object{
        lateinit var db: AppBlockerDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        db = AppBlockerDatabase.getDatabase(this)
    }
}