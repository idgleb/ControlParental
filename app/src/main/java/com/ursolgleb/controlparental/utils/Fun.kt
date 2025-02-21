package com.ursolgleb.controlparental.utils

import java.net.HttpURLConnection
import java.net.URL

class Fun {
    companion object {
        fun formatearTiempoDeUso(secondsDeUso: Long): String {
            val seconds = secondsDeUso % 60
            val minutes = (secondsDeUso / 60) % 60
            val hours = secondsDeUso / 3600
            val strSec = if (hours.toInt() != 0) "" else seconds.toString() + "seg"
            val strMin = if (minutes.toInt() == 0) "" else minutes.toString() + "min"
            val strHour = if (hours.toInt() == 0) "" else hours.toString() + "h"
            val formattedTimeDeUso = "$strHour $strMin $strSec"
            return formattedTimeDeUso
        }

        fun isUrlExists(url: String): Boolean {
            return try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    connectTimeout = 3000 // Reducimos timeout para mejorar rapidez
                    readTimeout = 3000
                    instanceFollowRedirects = true // Permite seguir redirecciones
                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
                    ) // User-Agent real
                }
                connection.responseCode in 200..399 // Acepta respuestas 2xx y redirecciones 3xx
            } catch (e: Exception) {
                false // Si hay error, asumimos que la URL no existe
            }
        }

    }
}