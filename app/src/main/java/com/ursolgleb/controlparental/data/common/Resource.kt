package com.ursolgleb.controlparental.data.common

/**
 * Clase sellada para representar el estado de un recurso de datos (Cargando, Éxito, Error).
 */
sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T?) : Resource<T>(data)
    class Loading<T>(data: T? = null) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
} 