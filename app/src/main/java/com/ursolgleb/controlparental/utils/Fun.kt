package com.ursolgleb.controlparental.utils

import java.net.HttpURLConnection
import java.net.URL

class Fun {
    companion object {
        fun formatearTiempoDeUso(miliSecondsDeUso: Long): String {
            val totalSeconds = miliSecondsDeUso / 1000 // Convertir ms a segundos
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60

            return when {
                hours > 0 -> "${hours}h ${if (minutes > 0) "$minutes min" else ""}".trim()
                minutes > 0 -> "$minutes min ${if (seconds > 0) "$seconds seg" else ""}".trim()
                else -> "$seconds seg"
            }
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