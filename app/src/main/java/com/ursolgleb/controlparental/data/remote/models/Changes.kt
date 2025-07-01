package com.ursolgleb.controlparental.data.remote.models

data class Changes(
    val added: List<Long>? = null,
    val updated: List<Long>? = null,
    val deleted: List<Long>? = null
) {
    fun getAddedSafe(): List<Long> = added ?: emptyList()
    fun getUpdatedSafe(): List<Long> = updated ?: emptyList()
    fun getDeletedSafe(): List<Long> = deleted ?: emptyList()
} 