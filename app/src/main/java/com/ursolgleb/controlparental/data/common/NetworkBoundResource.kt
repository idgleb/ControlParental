package com.ursolgleb.controlparental.data.common

import android.util.Log
import kotlinx.coroutines.flow.*
import retrofit2.Response

/**
 * Recurso genérico que puede proporcionar datos desde la BD local y actualizarlos desde la red.
 * Esta versión simplificada utiliza un constructor `flow` para un control claro y predecible.
 *
 * @param ResultType Tipo de los datos de la BD (Entidades).
 * @param RequestType Tipo de la respuesta de la API (DTOs).
 * @param query Función para obtener los datos desde la BD como un Flow.
 * @param fetch Función suspendida para obtener los datos desde la API.
 * @param saveFetchResult Función suspendida para guardar el resultado de la API en la BD.
 * @param shouldFetch Función para decidir si se debe buscar en la red.
 */
inline fun <ResultType, RequestType> networkBoundResource(
    crossinline query: () -> Flow<ResultType>,
    crossinline fetch: suspend () -> Response<RequestType>,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: (ResultType?) -> Boolean = { true }
): Flow<Resource<ResultType>> = flow {
    // 1. Empezar siempre emitiendo el estado de carga con los datos locales actuales.
    val startingData = query().firstOrNull()
    emit(Resource.Loading(startingData))

    // 2. Decidir si es necesario ir a la red.
    if (shouldFetch(startingData)) {
        try {
            // 3. Realizar la llamada a la red.
            val response = fetch()
            if (response.isSuccessful && response.body() != null) {
                // 4. Si es exitosa, guardar los nuevos datos.
                saveFetchResult(response.body()!!)
                // 5. Emitir los datos actualizados desde la fuente de verdad (la BD).
                emitAll(query().map { Resource.Success(it) })
            } else {
                // 6. Si la API falla, emitir un error, pero seguir mostrando los datos antiguos.
                Log.w("NetworkBoundResource", "API call failed with response: ${response.code()}")
                emit(Resource.Error("API Error: ${response.code()}", startingData))
            }
        } catch (e: Exception) {
            // Es crucial no tratar las excepciones de cancelación como errores.
            if (e is java.util.concurrent.CancellationException) {
                throw e
            }
            // 7. Si hay un error de red real, emitirlo y mostrar los datos antiguos.
            Log.e("NetworkBoundResource", "Network error", e)
            emit(Resource.Error(e.message ?: "Network error", startingData))
        }
    } else {
        // 8. Si no se necesita ir a la red, simplemente emitir los datos locales como exitosos.
        emitAll(query().map { Resource.Success(it) })
    }
}

/**
 * Clase sellada para representar el estado de un recurso de datos (Cargando, Éxito, Error).
 */
sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    class Success<T>(data: T?) : Resource<T>(data)
    class Loading<T>(data: T? = null) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
} 