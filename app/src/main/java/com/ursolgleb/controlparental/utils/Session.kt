package com.ursolgleb.controlparental.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Session @Inject constructor(@ApplicationContext context: Context) {
    private var sessionActive = false
    private var sessionStartTime: Long = 0
    private val sessionDuration = 1 * 60 * 1000 // 1 minutos en milisegundos

    fun isSessionActive(): Boolean {
        return if (sessionActive) {
            if (tiempoTranscurrido() < sessionDuration) {
                true
            } else {
                cerrarSesion()
                false
            }
        } else {
            false
        }
    }

    fun isSessionExpired(): Boolean {
        return sessionActive && System.currentTimeMillis() - sessionStartTime > sessionDuration
    }

    fun iniciarSesion() {
        if (sessionActive) {
            sessionStartTime = System.currentTimeMillis()
        } else {
            sessionActive = true
            sessionStartTime = System.currentTimeMillis()
        }
    }

    fun cerrarSesion() {
        sessionActive = false
        sessionStartTime = 0
    }

    fun tiempoTranscurrido(): Long {
        return if (sessionActive) {
            System.currentTimeMillis() - sessionStartTime
        } else {
            0
        }
    }


}