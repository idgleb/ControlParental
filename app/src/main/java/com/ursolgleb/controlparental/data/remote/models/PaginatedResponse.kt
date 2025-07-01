package com.ursolgleb.controlparental.data.remote.models

/**
 * Respuesta paginada genérica para endpoints que devuelven listas
 * Usado por:
 * - GET /api/sync/apps?deviceId={deviceId}&limit={limit}&offset={offset}
 * Ejemplo JSON:
 * {
 *   "data": [...],
 *   "pagination": {
 *     "total": 150,
 *     "limit": 25,
 *     "offset": 0,
 *     "hasMore": true
 *   }
 * }
 */
data class PaginatedResponse<T>(
    val data: List<T>,
    val pagination: Pagination
) 