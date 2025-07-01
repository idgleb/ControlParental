package com.ursolgleb.controlparental.data.common

/**
 * Clase sellada para representar el estado de un recurso de datos (Cargando, Éxito, Error)
 * Utilizada para envolver respuestas de red y operaciones de base de datos
 * 
 * Ejemplo de uso:
 * ```kotlin
 * when (resource) {
 *     is Resource.Success -> {
 *         // Operación exitosa, usar resource.data
 *         showData(resource.data)
 *     }
 *     is Resource.Error -> {
 *         // Error ocurrido, mostrar resource.message
 *         showError(resource.message)
 *         // Opcionalmente usar resource.data para datos en caché
 *         resource.data?.let { showCachedData(it) }
 *     }
 *     is Resource.Loading -> {
 *         // Operación en progreso
 *         showLoadingIndicator()
 *         // Opcionalmente mostrar resource.data (datos antiguos)
 *         resource.data?.let { showOldData(it) }
 *     }
 * }
 * ```
 * 
 * @param T Tipo de datos que contiene el recurso
 * @param data Los datos del recurso (puede ser null en estados Loading o Error)
 * @param message Mensaje de error (solo relevante para Resource.Error)
 */
sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T?) : Resource<T>(data)
    class Loading<T>(data: T? = null) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
} 