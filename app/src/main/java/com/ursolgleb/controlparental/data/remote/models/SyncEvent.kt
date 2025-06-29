package com.ursolgleb.controlparental.data.remote.models

data class SyncEvent(
    val id: Long,
    val deviceId: String,
    val entity_type: String,
    val entity_id: String,
    val action: String,
    val data: Map<String, Any>? = null,
    val previous_data: Map<String, Any>? = null,
    val created_at: String,
    val synced_at: String? = null
)

data class SyncEventsResponse(
    val status: String,
    val events: List<SyncEvent>,
    val lastEventId: Long,
    val hasMore: Boolean,
    val timestamp: String
)

data class ClientSyncEvent(
    val entity_type: String,
    val entity_id: String,
    val action: String,
    val data: Map<String, Any?>? = null,
    val timestamp: String
) 