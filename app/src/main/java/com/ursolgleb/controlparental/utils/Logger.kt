package com.ursolgleb.controlparental.utils

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object Logger {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun info(context: Context, tag: String, message: String) {
        logToFile(context, "ℹ️ $message")
        Log.i(tag, message)
    }

    fun warn(context: Context, tag: String, message: String) {
        logToFile(context, "⚠️ $message")
        Log.w(tag, message)
    }

    fun error(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        logToFile(context, "❌ $message")
        Log.e(tag, message, throwable)
    }

    private fun logToFile(context: Context, text: String) {
        scope.launch {
            try {
                Archivo.appendTextToFile(context, "\n$text")
            } catch (e: Exception) {
                Log.e("Logger", "No se pudo escribir en archivo: ${e.message}")
            }
        }
    }
}
