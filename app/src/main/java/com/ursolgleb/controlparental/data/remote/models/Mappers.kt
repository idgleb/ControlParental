package com.ursolgleb.controlparental.data.remote.models

import android.util.Log
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.HorarioEntity
import com.ursolgleb.controlparental.data.local.entities.DeviceEntity
import com.ursolgleb.controlparental.utils.StatusApp
import com.ursolgleb.controlparental.utils.Converters

/** Conversiones de entidades locales a DTOs y viceversa */

fun AppEntity.toDto(): AppDto {
    val iconByteArray = Converters.fromBitmap(appIcon)
    val iconIntList = iconByteArray.map { it.toInt() and 0xFF } // Convertir ByteArray a List<Int>
    
    return AppDto(
        packageName = packageName,
        deviceId = deviceId,
        appName = appName,
        appIcon = iconIntList,
        appCategory = appCategory,
        contentRating = contentRating,
        isSystemApp = isSystemApp,
        usageTimeToday = usageTimeToday,
        timeStempUsageTimeToday = timeStempUsageTimeToday,
        appStatus = appStatus,
        dailyUsageLimitMinutes = dailyUsageLimitMinutes
    )
}

fun AppDto.toEntity(): AppEntity? {
    val pkg = packageName ?: return null
    val devId = deviceId ?: return null
    
    // Manejar el ícono con más cuidado
    val iconBitmap = try {
        val iconIntList = appIcon
        if (iconIntList != null && iconIntList.isNotEmpty()) {
            // Convertir List<Int> a ByteArray
            val iconByteArray = ByteArray(iconIntList.size)
            iconIntList.forEachIndexed { index, value ->
                iconByteArray[index] = value.toByte()
            }
            Converters.toBitmap(iconByteArray)
        } else {
            Log.w("Mappers", "AppIcon is null or empty for $pkg")
            // Crear un bitmap gris por defecto de 35x35
            // TODO: En el futuro, podríamos intentar obtener el ícono del PackageManager
            // si la app está instalada en el dispositivo
            val defaultBitmap = android.graphics.Bitmap.createBitmap(35, 35, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(defaultBitmap)
            canvas.drawColor(android.graphics.Color.LTGRAY)
            defaultBitmap
        }
    } catch (e: Exception) {
        Log.e("Mappers", "Error converting icon for $pkg: ${e.message}")
        // Crear un bitmap gris en caso de error
        val defaultBitmap = android.graphics.Bitmap.createBitmap(35, 35, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(defaultBitmap)
        canvas.drawColor(android.graphics.Color.LTGRAY)
        defaultBitmap
    }
    
    return AppEntity(
        packageName = pkg,
        deviceId = devId,
        appName = appName ?: pkg,
        appIcon = iconBitmap,
        appCategory = appCategory ?: "Unknown",
        contentRating = contentRating ?: "Unknown",
        isSystemApp = isSystemApp,
        usageTimeToday = usageTimeToday ?: 0L,
        timeStempUsageTimeToday = timeStempUsageTimeToday,
        appStatus = appStatus ?: StatusApp.DEFAULT.desc,
        dailyUsageLimitMinutes = dailyUsageLimitMinutes ?: 0
    )
}

fun HorarioEntity.toDto() = HorarioDto(
    idHorario = idHorario,
    deviceId = deviceId,
    nombreDeHorario = nombreDeHorario,
    diasDeSemana = diasDeSemana,
    horaInicio = horaInicio.toString(),
    horaFin = horaFin.toString(),
    isActive = isActive
)

fun HorarioDto.toEntity(): HorarioEntity {
    return HorarioEntity(
        deviceId = this.deviceId ?: "",
        idHorario = this.idHorario,
        nombreDeHorario = this.nombreDeHorario,
        diasDeSemana = this.diasDeSemana,
        horaInicio = this.horaInicio ?: "00:00",
        horaFin = this.horaFin ?: "23:59",
        isActive = this.isActive
    )
}

fun DeviceEntity.toDto() = DeviceDto(
    deviceId = deviceId,
    model = model,
    batteryLevel = batteryLevel,
    latitude = latitude,
    longitude = longitude
)

fun DeviceDto.toEntity(): DeviceEntity? {
    val id = deviceId ?: return null
    val m = model ?: return null
    val level = batteryLevel ?: return null
    return DeviceEntity(
        deviceId = id,
        model = m,
        batteryLevel = level,
        latitude = latitude,
        longitude = longitude,
        locationUpdatedAt = if (latitude != null && longitude != null) System.currentTimeMillis() else null
    )
}

