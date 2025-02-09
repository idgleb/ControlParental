package com.ursolgleb.controlparental.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UpdateAppsBloquedasReceiver(private val actualizarListaAppsBloqueadas: (String) -> Unit) : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.ursolgleb.controlparental.UPDATE_BLOCKED_APPS") {
            val msg = intent.getStringExtra("mensaje").toString()
            actualizarListaAppsBloqueadas(msg)
        }
    }


}
