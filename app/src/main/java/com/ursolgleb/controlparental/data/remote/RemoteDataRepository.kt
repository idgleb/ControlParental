package com.ursolgleb.controlparental.data.remote

import android.util.Log
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

    suspend fun fetchHorarios(deviceId: String? = null): List<HorarioDto> {
        val response = api.getHorarios(deviceId)
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            Log.e("RemoteRepo", "Error: ${response.code()} ${response.message()}")
            throw Exception("Failed to fetch horarios: ${response.message()}")
        }
    }

    suspend fun fetchDevice(deviceId: String? = null): List<DeviceDto> {
        val response = api.getDevice(deviceId)
        if (response.isSuccessful) {
            return response.body() ?: emptyList()
        } else {
            Log.e("RemoteRepo", "Error: ${response.code()} ${response.message()}")
            throw Exception("Failed to fetch device: ${response.message()}")
        }
    }


    suspend fun pushApps(apps: List<AppDto>) = api.postApps(apps)

    suspend fun pushDevice(device: DeviceDto) = api.postDevice(device)

    suspend fun pushHorarios(horarios: List<HorarioDto>) = api.postHorarios(horarios)


    suspend fun deleteHorarios(deviceIds: List<String>) = api.deleteHorarios(deviceIds)

    suspend fun deleteApps(deviceIds: List<String>) = api.deleteApps(deviceIds)



}