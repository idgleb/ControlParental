package com.ursolgleb.controlparental.data.remote.models

// Moshi reflexión: no necesitamos Json

data class SyncResponse<T>(
    val status: String,

    val data: List<T>? = null,

    val changes: Changes? = null,

    val timestamp: String? = null,

    val totalChanges: Int? = null,

    val message: String? = null,

    val deviceId: String? = null,

    val error: String? = null,

    val details: String? = null
)