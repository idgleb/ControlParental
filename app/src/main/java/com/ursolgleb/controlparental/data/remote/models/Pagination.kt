package com.ursolgleb.controlparental.data.remote.models

/**
 * Información de paginación para respuestas con listas grandes
 * Ejemplo JSON:
 * {
 *   "total": 150,
 *   "limit": 25,
 *   "offset": 0,
 *   "hasMore": true
 * }
 */
data class Pagination(
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
) 