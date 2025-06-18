package com.ursolgleb.controlparental.data.remote

import com.ursolgleb.controlparental.data.remote.api.LaravelApi
import com.ursolgleb.controlparental.data.remote.models.AppDto
import com.ursolgleb.controlparental.data.remote.models.HorarioDto
import com.ursolgleb.controlparental.data.remote.models.DeviceDto
import javax.inject.Inject

/**
 * Repositorio encargado de comunicar la aplicación con el backend Laravel.
 */
class RemoteDataRepository @Inject constructor(
    private val api: LaravelApi
) {

    suspend fun fetchApps(deviceId: String? = null): List<AppDto> {
        return api.getApps(deviceId)
    }

    suspend fun pushApps(apps: List<AppDto>) = api.postApps(apps)

    suspend fun deleteApps(deviceIds: List<String>) = api.deleteApps(deviceIds)

    suspend fun fetchHorarios(deviceId: String? = null): List<HorarioDto> {
        return api.getHorarios(deviceId)
    }

    suspend fun pushHorarios(horarios: List<HorarioDto>) = api.postHorarios(horarios)

    suspend fun deleteHorarios(deviceIds: List<String>) = api.deleteHorarios(deviceIds)

    suspend fun fetchDevice(): DeviceDto = api.getDevice()

    suspend fun pushDevice(device: DeviceDto) = api.postDevice(device)

}