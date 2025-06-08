package com.ursolgleb.controlparental.data.remote

import com.ursolgleb.controlparental.data.remote.api.LaravelApi
import com.ursolgleb.controlparental.data.remote.models.AppDto
import com.ursolgleb.controlparental.data.remote.models.HorarioDto
import javax.inject.Inject

/**
 * Repositorio encargado de comunicar la aplicación con el backend Laravel.
 */
class RemoteDataRepository @Inject constructor(
    private val api: LaravelApi
) {

    suspend fun fetchApps(): List<AppDto> = api.getApps()

    suspend fun pushApps(apps: List<AppDto>) = api.postApps(apps)

    suspend fun fetchHorarios(): List<HorarioDto> = api.getHorarios()

    suspend fun pushHorarios(horarios: List<HorarioDto>) = api.postHorarios(horarios)
}