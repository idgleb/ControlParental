package com.ursolgleb.controlparental.data.remote.models

/**
 * Representa un evento individual que se sincroniza.
 */
data class EventDto(
    val entity_type: String,
    val entity_id: String,
    val action: String,
    val data: Map<String, Any?>? = null,
    val timestamp: String
) 