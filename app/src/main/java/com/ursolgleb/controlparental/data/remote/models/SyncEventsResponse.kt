package com.ursolgleb.controlparental.data.remote.models

data class SyncEventsResponse(
    val status: String,
    val events: List<SyncEvent>,
    val lastEventId: Long,
    val hasMore: Boolean,
    val timestamp: String
) 