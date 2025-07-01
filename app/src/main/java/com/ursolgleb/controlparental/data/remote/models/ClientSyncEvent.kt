package com.ursolgleb.controlparental.data.remote.models

data class ClientSyncEvent(
    val entity_type: String,
    val entity_id: String,
    val action: String,
    val data: Map<String, Any?>? = null,
    val timestamp: String
) 