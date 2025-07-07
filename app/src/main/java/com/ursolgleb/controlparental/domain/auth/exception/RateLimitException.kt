package com.ursolgleb.controlparental.domain.auth.exception

/**
 * Excepción lanzada cuando se excede el límite de peticiones (HTTP 429)
 */
class RateLimitException(
    val retryAfterSeconds: Int,
    message: String = "Demasiados intentos. Por favor, espere $retryAfterSeconds segundos antes de intentar nuevamente."
) : Exception(message) 