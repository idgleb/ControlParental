package com.ursolgleb.controlparental.data.remote.models

import android.graphics.Bitmap
import com.ursolgleb.controlparental.data.apps.entities.AppEntity
import com.ursolgleb.controlparental.data.apps.entities.HorarioEntity
import com.ursolgleb.controlparental.utils.StatusApp
import com.ursolgleb.controlparental.utils.Converters

/** Conversiones de entidades locales a DTOs y viceversa */

fun AppEntity.toDto() = AppDto(
    packageName = packageName,
    appName = appName,
    appIcon = Converters.fromBitmap(appIcon),
    appCategory = appCategory,
    contentRating = contentRating,
    isSystemApp = isSystemApp,
    usageTimeToday = usageTimeToday,
    timeStempUsageTimeToday = timeStempUsageTimeToday,
    appStatus = appStatus,
    dailyUsageLimitMinutes = dailyUsageLimitMinutes
)

fun AppDto.toEntity(): AppEntity? {
    val pkg = packageName ?: return null
    val iconBytes = appIcon ?: return null
    return AppEntity(
        packageName = pkg,
        appName = appName ?: pkg,
        appIcon = Converters.toBitmap(iconBytes),
        appCategory = appCategory,
        contentRating = contentRating,
        isSystemApp = isSystemApp,
        usageTimeToday = usageTimeToday ?: 0L,
        timeStempUsageTimeToday = timeStempUsageTimeToday,
        appStatus = appStatus ?: StatusApp.DEFAULT.desc,
        dailyUsageLimitMinutes = dailyUsageLimitMinutes ?: 0
    )
}

fun HorarioEntity.toDto() = HorarioDto(
    id = id,
    nombreDeHorario = nombreDeHorario,
    diasDeSemana = diasDeSemana,
    horaInicio = horaInicio.toString(),
    horaFin = horaFin.toString(),
    isActive = isActive
)

fun HorarioDto.toEntity(): HorarioEntity? {
    val inicio = horaInicio?.let { java.time.LocalTime.parse(it) } ?: return null
    val fin = horaFin?.let { java.time.LocalTime.parse(it) } ?: return null
    return HorarioEntity(
        id = id,
        nombreDeHorario = nombreDeHorario,
        diasDeSemana = diasDeSemana,
        horaInicio = inicio,
        horaFin = fin,
        isActive = isActive
    )
}