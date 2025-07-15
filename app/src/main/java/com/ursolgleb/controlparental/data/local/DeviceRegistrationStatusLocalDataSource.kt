package com.ursolgleb.controlparental.data.local

import android.content.SharedPreferences
import javax.inject.Inject

class DeviceRegistrationStatusLocalDataSource @Inject constructor(
    private val sharedPrefs: SharedPreferences
) {
    private val KEY_FALLIDO = "registro_fallido"
    private val KEY_TIMESTAMP = "registro_fallido_timestamp"

    fun marcarRegistroFallido() {
        sharedPrefs.edit()
            .putBoolean(KEY_FALLIDO, true)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun limpiarRegistroFallido() {
        sharedPrefs.edit()
            .remove(KEY_FALLIDO)
            .remove(KEY_TIMESTAMP)
            .apply()
    }

    fun puedeIntentarRegistro(tiempoEsperaMs: Long = 5 * 1000): Boolean {
        val registroFallido = sharedPrefs.getBoolean(KEY_FALLIDO, false)
        val timestamp = sharedPrefs.getLong(KEY_TIMESTAMP, 0L)
        return !registroFallido || (System.currentTimeMillis() - timestamp > tiempoEsperaMs)
    }
} 