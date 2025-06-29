package com.ursolgleb.controlparental.utils

import android.util.Log
import android.util.TypedValue
import android.view.View
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.util.Calendar
import java.util.Date
import java.util.Locale

class Fun {
    companion object {

        val dateFormat =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) // Formato con fecha y hora

        fun getTimeAtras(dias: Int): Long {
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, -dias) // Retrocede X días
                set(Calendar.HOUR_OF_DAY, 0) // Establece la hora a 00
                set(Calendar.MINUTE, 0) // Establece los minutos a 0
                set(Calendar.SECOND, 0) // Establece los segundos a 0
                set(Calendar.MILLISECOND, 0) // Establece los milisegundos a 0
            }
            Log.d("getTimeMesAtras", "getTimeMesAtras ${dateFormat.format(calendar.timeInMillis)}")
            return calendar.timeInMillis
        }

        fun formatearMiliSec(miliSecondsDeUso: Long): String {
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

        fun dpToPx(dp: Int, view: View): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,  // Unidad DIP (dp)
                dp.toFloat(),                 // Valor a convertir
                view.resources.displayMetrics  // Densidad de la pantalla
            ).toInt()
        }

        fun getHoraActual(): LocalTime {
            return LocalTime.now()
        }

        fun getDiaDeLaSemana(): Int {
            val calendar = Calendar.getInstance()
            val dia = calendar.get(Calendar.DAY_OF_WEEK)
            return if (dia == Calendar.SUNDAY) 7 else dia - 1 // 1 = lunes, ..., 7 = domingo
        }

        fun estaDentroDelHorario(horaActual: LocalTime, horaInicio: String, horaFin: String): Boolean {
            val inicio = LocalTime.parse(horaInicio)
            val fin = LocalTime.parse(horaFin)
            return horaActual.isAfter(inicio) && horaActual.isBefore(fin)
        }

        fun millisToFormattedDate(millis: Long, pattern: String = "dd/MM/yyyy HH:mm:ss"): String {
            val formatter = SimpleDateFormat(pattern, Locale.getDefault())
            return formatter.format(Date(millis))
        }

    }
}

private fun LocalTime.isAfter(string: String) {}
