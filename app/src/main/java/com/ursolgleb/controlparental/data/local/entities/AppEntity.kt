package com.ursolgleb.controlparental.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.ursolgleb.controlparental.Converters

@Entity(tableName = "apps")
@TypeConverters(Converters::class)  // 💡 Importante para que Room use el JSON
data class AppEntity(
    @PrimaryKey val packageName: String,
    var appName: String,
    var appIcon: String,
    var appCategory: String,
    var contentRating: String,
    var appIsSystemApp: Boolean,
    var tiempoUsoHoy: Long,
    var timeStempToday: Long,
    var blocked: Boolean,
    var usoLimitPorDiaMinutos: Int,
    var entretenimiento: Boolean
)
