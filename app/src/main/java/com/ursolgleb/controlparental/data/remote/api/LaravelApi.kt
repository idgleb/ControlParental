package com.ursolgleb.controlparental.data.remote.api

import com.ursolgleb.controlparental.data.remote.models.AppDto
import com.ursolgleb.controlparental.data.remote.models.HorarioDto
import com.ursolgleb.controlparental.data.remote.models.DeviceDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Definición básica de la API remota en Laravel.
 * Las rutas deben existir en el backend para que la sincronización funcione.
 */
interface LaravelApi {

    @GET("sync/apps")
    suspend fun getApps(@Query("deviceId") deviceId: String?): List<AppDto>

    @POST("sync/apps")
    suspend fun postApps(@Body apps: List<AppDto>)

    @DELETE("sync/apps")
    suspend fun deleteApps(@Query("deviceId") deviceIds: List<String>)

    @GET("sync/horarios")
    suspend fun getHorarios(@Query("deviceId") deviceId: String?): Response<List<HorarioDto>>

    @GET("sync/devices")
    suspend fun getDevice(@Query("deviceId") deviceId: String?): Response<List<DeviceDto>>


    @POST("sync/horarios")
    suspend fun postHorarios(@Body horarios: List<HorarioDto>)

    @DELETE("sync/horarios")
    suspend fun deleteHorarios(@Query("deviceId") deviceIds: List<String>)

    @POST("sync/devices")
    suspend fun postDevice(@Body device: DeviceDto)

}