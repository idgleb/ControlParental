package com.ursolgleb.controlparental.data.remote.models

import com.ursolgleb.controlparental.data.apps.entities.AppEntity
import com.ursolgleb.controlparental.data.apps.entities.HorarioEntity
import com.ursolgleb.controlparental.utils.StatusApp

/** Conversiones de entidades locales a DTOs y viceversa */

fun AppEntity.toDto() = AppDto(
    packageName = packageName,
    appName = appName,
    appStatus = appStatus,
    dailyUsageLimitMinutes = dailyUsageLimitMinutes,
    usageTimeToday = usageTimeToday
)

fun AppDto.toEntity(iconPlaceholder: android.graphics.Bitmap): AppEntity? {
    val pkg = packageName ?: return null
    return AppEntity(
        packageName = pkg,
        appName = appName ?: pkg,
        appIcon = iconPlaceholder,
        appCategory = "remote",
        contentRating = "?",
        isSystemApp = false,
        usageTimeToday = usageTimeToday ?: 0L,
        timeStempUsageTimeToday = System.currentTimeMillis(),
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

fun HorarioDto.toEntity() = HorarioEntity(
    id = id,
    nombreDeHorario = nombreDeHorario,
    diasDeSemana = diasDeSemana,
    horaInicio = java.time.LocalTime.parse(horaInicio),
    horaFin = java.time.LocalTime.parse(horaFin),
    isActive = isActive
)