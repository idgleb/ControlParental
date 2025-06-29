package com.ursolgleb.controlparental.data.remote.models
import com.squareup.moshi.Json

/**
 * Payload que se envía a POST /api/sync/events
 */
data class PostEventsRequest(
    val deviceId: String,
    val events: List<EventDto>
)

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