package com.ursolgleb.controlparental.data.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.Response

/**
 * Recurso genérico que puede proporcionar datos desde la base de datos local
 * y actualizarlos desde la red.
 *
 * @param <ResultType>  Tipo de los datos de la BD (Entidades)
 * @param <RequestType> Tipo de la respuesta de la API (DTOs)
 */
inline fun <ResultType, RequestType> networkBoundResource(
    // Función para obtener los datos desde la BD
    crossinline query: () -> Flow<ResultType>,
    // Función para obtener los datos desde la API
    crossinline fetch: suspend () -> Response<RequestType>,
    // Función para guardar el resultado de la API en la BD
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    // Función para decidir si se debe buscar en la red
    crossinline shouldFetch: (ResultType) -> Boolean = { true },
    // Función para manejar errores de la API
    crossinline onFetchFailed: (Throwable) -> Unit = { }
) = flow {
    // 1. Emitir los datos locales primero
    val data = query().first()
    emit(Resource.Loading(data))

    // 2. Decidir si ir a la red
    if (shouldFetch(data)) {
        // 3. Obtener datos de la red
        try {
            val response = fetch()
            if (response.isSuccessful) {
                // 4. Guardar datos en la BD y emitir de nuevo
                response.body()?.let { saveFetchResult(it) }
                emitAll(query().map { Resource.Success(it) })
            } else {
                // Error de la API
                val errorBody = response.errorBody()?.string()
                onFetchFailed(Exception(errorBody ?: "Unknown API error"))
                emitAll(query().map { Resource.Error("API Error: ${response.code()}", it) })
            }
        } catch (t: Throwable) {
            // Error de red
            onFetchFailed(t)
            emitAll(query().map { Resource.Error(t.message ?: "Network error", it) })
        }
    } else {
        // 5. No ir a la red, simplemente emitir los datos locales
        emitAll(query().map { Resource.Success(it) })
    }
}

/**
 * Clase sellada para representar el estado de un recurso de datos.
 */
sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T) : Resource<T>(data)
    class Loading<T>(data: T? = null) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
} 