package com.ursolgleb.controlparental

import android.content.Context
import android.util.Log
import com.ursolgleb.controlparental.UI.MainActivity
import com.ursolgleb.controlparental.utils.Archivo

class allowedApps {
    companion object{
        var apps = mutableListOf(
        "com.ursolgleb.appblocker",
        "com.ursolgleb.controlparental",
        "com.android.chrome",
        "com.google.android.apps.nexuslauncher",
        "com.android.settings",
        "com.android.systemui",
        "com.google.android.inputmethod.latin"
        )

        fun showApps(context: Context){
            var msg = "Lista de aplicaciones acceptadas:"
            Log.d("AppBlockerService", msg)
            Archivo.appendTextToFile(context, MainActivity.fileName, "\n $msg")
            apps.forEach {
                Log.w("AppBlockerService", it)
                Archivo.appendTextToFile(context, MainActivity.fileName, "\n $it")
            }
        }

    }
}