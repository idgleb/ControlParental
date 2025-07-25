package com.ursolgleb.controlparental.data.remote.api

import com.ursolgleb.controlparental.data.remote.models.AppDto
import com.ursolgleb.controlparental.data.remote.models.HorarioDto
import com.ursolgleb.controlparental.data.remote.models.DeviceDto
import com.ursolgleb.controlparental.data.remote.models.SyncResponse
import com.ursolgleb.controlparental.data.remote.models.PaginatedResponse
import com.ursolgleb.controlparental.data.remote.models.SyncEventsResponse
import com.ursolgleb.controlparental.data.remote.models.SyncStatusResponse
import com.ursolgleb.controlparental.data.remote.models.PostEventsRequest
import com.ursolgleb.controlparental.data.remote.models.HeartbeatRequest
import com.ursolgleb.controlparental.data.remote.models.HeartbeatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Definición básica de la API remota en Laravel.
 * Las rutas deben existir en el backend para que la sincronización funcione.
 */
interface LaravelApi {

    @GET("v1/sync/apps")
    suspend fun getApps(
        @Query("deviceId") deviceId: String?,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("includeIcons") includeIcons: Boolean = true
    ): PaginatedResponse<AppDto>

    @POST("v1/sync/apps")
    suspend fun postApps(@Body apps: List<AppDto>)

    @DELETE("v1/sync/apps")
    suspend fun deleteApps(@Query("deviceId") deviceIds: List<String>)

    @GET("v1/sync/horarios")
    suspend fun getHorarios(
        @Query("deviceId") deviceId: String?,
        @Query("lastSync") lastSync: String? = null,
        @Query("knownIds") knownIds: String? = null
    ): Response<SyncResponse<HorarioDto>>

    @GET("v1/sync/devices")
    suspend fun getDevice(@Query("deviceId") deviceId: String?): Response<List<DeviceDto>>


    @POST("v1/sync/horarios")
    suspend fun postHorarios(@Body horarios: List<HorarioDto>)

    @DELETE("v1/sync/horarios")
    suspend fun deleteHorarios(@Query("deviceId") deviceIds: List<String>)

    @POST("v1/sync/devices")
    suspend fun postDevice(@Body device: DeviceDto)

    // Nueva API de sincronización basada en eventos
    @GET("v1/sync/events")
    suspend fun getEvents(
        @Query("deviceId") deviceId: String,
        @Query("lastEventId") lastEventId: Long,
        @Query("types") types: String
    ): SyncEventsResponse

    @POST("v1/sync/events")
    suspend fun postEvents(@Body request: PostEventsRequest): Response<Unit>

    @GET("v1/sync/status")
    suspend fun getSyncStatus(@Query("deviceId") deviceId: String): SyncStatusResponse

    // Heartbeat para mantener dispositivo online
    @POST("v1/devices/{deviceId}/heartbeat")
    suspend fun sendHeartbeat(
        @Path("deviceId") deviceId: String,
        @Body request: HeartbeatRequest
    ): Response<HeartbeatResponse>

}