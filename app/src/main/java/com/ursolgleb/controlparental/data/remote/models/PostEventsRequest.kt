package com.ursolgleb.controlparental.data.remote.models

/**
 * Payload que se envía a POST /api/sync/events
 */
data class PostEventsRequest(
    val deviceId: String,
    val events: List<EventDto>
)