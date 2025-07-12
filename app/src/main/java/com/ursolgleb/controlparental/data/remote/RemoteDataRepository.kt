package com.ursolgleb.controlparental.data.remote

import android.util.Log
import com.ursolgleb.controlparental.data.remote.api.LaravelApi
import com.ursolgleb.controlparental.data.remote.models.AppDto
import com.ursolgleb.controlparental.data.remote.models.HorarioDto
import com.ursolgleb.controlparental.data.remote.models.DeviceDto
import com.ursolgleb.controlparental.data.remote.models.SyncResponse
import com.ursolgleb.controlparental.data.remote.models.SyncEventsResponse
import com.ursolgleb.controlparental.data.remote.models.SyncStatusResponse
import com.ursolgleb.controlparental.data.remote.models.PostEventsRequest
import com.ursolgleb.controlparental.data.remote.models.HeartbeatResponse
import com.ursolgleb.controlparental.data.remote.models.HeartbeatRequest
import kotlinx.coroutines.delay
import retrofit2.Response
import javax.inject.Inject

/**
 * Repositorio encargado de comunicar la aplicación con el backend Laravel.
 */
class RemoteDataRepository @Inject constructor(
    val api: LaravelApi
) {

    suspend fun fetchApps(deviceId: String? = null, includeIcons: Boolean = true): List<AppDto> {
        val allApps = mutableListOf<AppDto>()
        var offset = 0
        val limit = if (includeIcons) 12 else 50
        var hasMore = true
        var retryCount = 0
        val maxRetries = 3
        
        try {
            while (hasMore) {
                try {
                    Log.d("RemoteRepo", "Fetching apps with offset=$offset, limit=$limit, includeIcons=$includeIcons")
                    val response = api.getApps(deviceId, limit, offset, includeIcons)
                    Log.d("RemoteRepo", "Response received - data size: ${response.data.size}, pagination: ${response.pagination}")
                    allApps.addAll(response.data)
                    hasMore = response.pagination.hasMore
                    offset += limit
                    retryCount = 0
                    
                    Log.d("RemoteRepo", "Fetched ${response.data.size} apps, total so far: ${allApps.size}, hasMore: $hasMore")
                    
                    if (hasMore) {
                        delay(300)
                    }
                } catch (e: Exception) {
                    retryCount++
                    if (retryCount <= maxRetries) {
                        Log.w("RemoteRepo", "Error fetching apps at offset=$offset, retry $retryCount/$maxRetries: ${e.message}")
                        delay(1000L * retryCount)
                        continue
                    } else {
                        throw e
                    }
                }
            }
            
            Log.d("RemoteRepo", "Total apps fetched: ${allApps.size}")
        } catch (e: Exception) {
            Log.e("RemoteRepo", "Error fetching apps at offset=$offset after $maxRetries retries: ${e.message}")
            if (allApps.isEmpty()) {
                throw e
            }
        }
        
        return allApps
    }

    // Método legacy para compatibilidad hacia atrás
    suspend fun fetchHorarios(deviceId: String? = null): List<HorarioDto> {
        val response = api.getHorarios(deviceId)
        if (response.isSuccessful) {
            val syncResponse = response.body()
            return syncResponse?.data ?: emptyList()
        } else {
            Log.e("RemoteRepo", "Error: ${response.code()} ${response.message()}")
            throw Exception("Failed to fetch horarios: ${response.message()}")
        }
    }
    
    // Nuevo método con soporte para sincronización incremental
    suspend fun fetchHorarios(
        deviceId: String? = null,
        lastSync: String? = null,
        knownIds: List<Long>? = null
    ): SyncResponse<HorarioDto> {
        val knownIdsString = knownIds?.joinToString(",")
        val response = api.getHorarios(deviceId, lastSync, knownIdsString)
        if (response.isSuccessful) {
            return response.body()!!
        }
        throw Exception("Failed to fetch horarios: ${response.message()}")
    }

    suspend fun fetchDevice(deviceId: String? = null): List<DeviceDto> {
        val response = api.getDevice(deviceId)
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        }
        throw Exception("Failed to fetch device: ${response.message()}")
    }


    suspend fun pushApps(apps: List<AppDto>) = api.postApps(apps)

    suspend fun pushDevice(device: DeviceDto) = api.postDevice(device)

    suspend fun pushHorarios(horarios: List<HorarioDto>) = api.postHorarios(horarios)


    suspend fun deleteHorarios(deviceIds: List<String>) = api.deleteHorarios(deviceIds)

    suspend fun deleteApps(deviceIds: List<String>) = api.deleteApps(deviceIds)
    
    // Nueva API de sincronización basada en eventos
    suspend fun getEvents(
        deviceId: String, 
        lastEventId: Long,
        types: String
    ): SyncEventsResponse {
        return api.getEvents(deviceId, lastEventId, types)
    }
    
    suspend fun postEvents(request: PostEventsRequest): Response<Unit> {
        return api.postEvents(request)
    }
    
    suspend fun getSyncStatus(deviceId: String): SyncStatusResponse {
        return api.getSyncStatus(deviceId)
    }
    
    @Suppress("UNUSED_PARAMETER")
    suspend fun deleteAppByPackageName(deviceId: String, packageName: String) {
        // Por ahora no implementado, se puede agregar si es necesario
    }

    /**
     * Envía un heartbeat al servidor para mantener el dispositivo online
     */
    suspend fun sendHeartbeat(
        deviceId: String,
        latitude: Double? = null,
        longitude: Double? = null
    ): Response<HeartbeatResponse> {
        val request = HeartbeatRequest(
            latitude = latitude,
            longitude = longitude
        )
        
        return api.sendHeartbeat(deviceId, request)
    }

}