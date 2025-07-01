package com.ursolgleb.controlparental.data.remote.models

data class Pagination(
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
) 