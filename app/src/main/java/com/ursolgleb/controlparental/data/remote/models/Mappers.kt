package com.ursolgleb.controlparental.data.remote.models

import com.ursolgleb.controlparental.data.apps.entities.AppEntity
import com.ursolgleb.controlparental.data.apps.entities.HorarioEntity

/** Conversiones de entidades locales a DTOs y viceversa */

fun AppEntity.toDto() = AppDto(
    packageName = packageName,
    appName = appName,
    appStatus = appStatus,
    dailyUsageLimitMinutes = dailyUsageLimitMinutes,
    usageTimeToday = usageTimeToday
)

fun AppDto.toEntity(iconPlaceholder: android.graphics.Bitmap) = AppEntity(
    packageName = packageName,
    appName = appName,
    appIcon = iconPlaceholder,
    appCategory = "remote",
    contentRating = "?",
    isSystemApp = false,
    usageTimeToday = usageTimeToday,
    timeStempUsageTimeToday = System.currentTimeMillis(),
    appStatus = appStatus,
    dailyUsageLimitMinutes = dailyUsageLimitMinutes
)

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