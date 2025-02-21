package com.ursolgleb.controlparental.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    var appName: String,
    var appIcon: String,
    var appCategory: String,
    var contentRating: String,
    var appIsSystemApp: Boolean,
    var tiempoUsoSegundosHoy: Long,
    var tiempoUsoSegundosSemana: Long,
    var tiempoUsoSegundosMes: Long,
    var blocked: Boolean,
    var usoLimitPorDiaMinutos: Int,
    var entretenimiento: Boolean
)
