package com.ursolgleb.controlparental.data.remote.models

data class PaginatedResponse<T>(
    val data: List<T>,
    val pagination: Pagination
) 