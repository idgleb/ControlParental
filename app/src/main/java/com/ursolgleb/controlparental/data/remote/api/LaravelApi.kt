package com.ursolgleb.controlparental.data.remote.api

import com.ursolgleb.controlparental.data.remote.models.AppDto
import com.ursolgleb.controlparental.data.remote.models.HorarioDto
import com.ursolgleb.controlparental.data.remote.models.DeviceDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Definición básica de la API remota en Laravel.
 * Las rutas deben existir en el backend para que la sincronización funcione.
 */
interface LaravelApi {

    @GET("sync/apps")
    suspend fun getApps(): List<AppDto>

    @POST("sync/apps")
    suspend fun postApps(@Body apps: List<AppDto>)

    @GET("sync/horarios")
    suspend fun getHorarios(): List<HorarioDto>

    @POST("sync/horarios")
    suspend fun postHorarios(@Body horarios: List<HorarioDto>)

    @GET("sync/device")
    suspend fun getDevice(): DeviceDto

    @POST("sync/device")
    suspend fun postDevice(@Body device: DeviceDto)

}