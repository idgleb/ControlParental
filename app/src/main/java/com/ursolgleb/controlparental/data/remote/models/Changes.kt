package com.ursolgleb.controlparental.data.remote.models

/**
 * Resumen de cambios realizados durante una sincronización
 * Ejemplo JSON:
 * {
 *   "added": [1, 2, 3],
 *   "updated": [4, 5, 6],
 *   "deleted": [7, 8]
 * }
 * 
 * Los números representan IDs de las entidades afectadas
 */
data class Changes(
    val added: List<Long>? = null,
    val updated: List<Long>? = null,
    val deleted: List<Long>? = null
) {
    fun getAddedSafe(): List<Long> = added ?: emptyList()
    fun getUpdatedSafe(): List<Long> = updated ?: emptyList()
    fun getDeletedSafe(): List<Long> = deleted ?: emptyList()
} 