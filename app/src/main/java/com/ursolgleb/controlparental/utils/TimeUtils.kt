package com.ursolgleb.controlparental.utils

/**
 * Formatear segundos en un mensaje legible en español
 */
fun Int.toWaitTimeMessage(): String {
    return when {
        this <= 0 -> "Por favor intenta nuevamente"
        this < 60 -> "Por favor espera $this segundo${if (this != 1) "s" else ""}"
        else -> {
            val minutes = this / 60
            val seconds = this % 60
            when {
                seconds == 0 -> "Por favor espera $minutes minuto${if (minutes != 1) "s" else ""}"
                else -> "Por favor espera $minutes minuto${if (minutes != 1) "s" else ""} y $seconds segundo${if (seconds != 1) "s" else ""}"
            }
        }
    }
}

/**
 * Crear mensaje de error de rate limiting
 */
fun createRateLimitMessage(retryAfterSeconds: Int): String {
    return "Has realizado demasiados intentos. ${retryAfterSeconds.toWaitTimeMessage()} antes de intentar nuevamente."
} 