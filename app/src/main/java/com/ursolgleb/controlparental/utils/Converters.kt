package com.ursolgleb.controlparental.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream
import java.time.LocalTime

class Converters {
    @TypeConverter
    fun fromBitmap(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    @TypeConverter
    fun toBitmap(byteArray: ByteArray): Bitmap {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    @TypeConverter
    fun fromDiasList(dias: List<Int>): String {
        return dias.joinToString(",")
    }

    @TypeConverter
    fun toDiasList(data: String): List<Int> {
        return if (data.isEmpty()) emptyList() else data.split(",").map { it.toInt() }
    }

    @TypeConverter
    fun fromLocalTime(time: LocalTime): String {
        return time.toString()
    }

    @TypeConverter
    fun toLocalTime(timeString: String): LocalTime {
        return LocalTime.parse(timeString)
    }
}
