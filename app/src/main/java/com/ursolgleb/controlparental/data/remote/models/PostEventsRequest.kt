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
    @field:Json(name = "entity_type") val entityType: String,
    @field:Json(name = "entity_id") val entityId: String,
    val action: String,
    val data: Map<String, Any?>? = null,
    val timestamp: String
) 