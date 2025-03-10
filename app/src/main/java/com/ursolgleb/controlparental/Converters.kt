package com.ursolgleb.controlparental

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromJsonToMap(value: String?): Map<Int, Long> {
        return value?.let {
            val type = object : TypeToken<Map<Int, Long>>() {}.type
            gson.fromJson(value, type)
        } ?: emptyMap()
    }

    @TypeConverter
    fun fromMapToJson(map: Map<Int, Long>?): String {
        return gson.toJson(map)
    }
}

