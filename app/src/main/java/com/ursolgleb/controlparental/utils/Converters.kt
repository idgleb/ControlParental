package com.ursolgleb.controlparental.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.graphics.scale
import androidx.room.TypeConverter
import java.io.ByteArrayOutputStream
import java.time.LocalTime

object  Converters {
    @TypeConverter
//    fun fromBitmap(bitmap: Bitmap): ByteArray {
//        val stream = ByteArrayOutputStream()
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
//        return stream.toByteArray()
//    }
    fun fromBitmap(bitmap: Bitmap): ByteArray {
        val scaledBitmap = bitmap.scale(35, 35) // Mantener 35x35
        val stream = ByteArrayOutputStream()
        
        // Usar WebP con 70% de calidad para máxima compresión
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // WebP con pérdida (más compresión) disponible desde Android 11
            scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 70, stream)
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            // WebP básico disponible desde Android 4.3
            @Suppress("DEPRECATION")
            scaledBitmap.compress(Bitmap.CompressFormat.WEBP, 70, stream)
        } else {
            // Fallback a JPEG para versiones muy antiguas
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        }
        
        return stream.toByteArray()
    }

    @TypeConverter
    fun toBitmap(byteArray: ByteArray): Bitmap {
        // BitmapFactory maneja automáticamente WebP, JPEG y PNG
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            ?: throw IllegalArgumentException("No se pudo decodificar la imagen")
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

    // Método adicional con calidad configurable
    fun fromBitmapWithQuality(bitmap: Bitmap, quality: Int = 70): ByteArray {
        val scaledBitmap = bitmap.scale(35, 35)
        val stream = ByteArrayOutputStream()
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, stream)
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            @Suppress("DEPRECATION")
            scaledBitmap.compress(Bitmap.CompressFormat.WEBP, quality, stream)
        } else {
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        }
        
        return stream.toByteArray()
    }
}
