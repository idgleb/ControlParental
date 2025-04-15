package com.ursolgleb.controlparental.utils

import android.content.Context
import android.util.Log
import com.ursolgleb.controlparental.UI.activities.DesarolloActivity
import java.io.File
import java.io.IOException

class Archivo {
    companion object {

        fun appendTextToFile(context: Context, text: String) {
            val file = File(context.filesDir, DesarolloActivity.fileName)

            try {
                // Asegura que el archivo exista
                if (!file.exists()) {
                    file.parentFile?.mkdirs() // crea directorio si no existe
                    file.createNewFile()
                }

                // Limita el tamaño del archivo (0.5 MB = 500,152 bytes)
                if (file.length() >= 500_152) {
                    file.writeText("") // limpia el contenido
                }

                file.appendText(text) // escribe el texto nuevo
            } catch (e: IOException) {
                Log.e("Archivo", "Error al escribir en el archivo: ${e.message}", e)
            }
        }



        fun readTextFromFile(context: Context, fileName: String): String {
            val file = File(context.filesDir, fileName)
            return if (file.exists()) {
                file.readText() // Lee el contenido del archivo
            } else {
                "Archivo no encontrado"
            }
        }

        fun clearFile(context: Context, fileName: String) {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                file.writeText("") // Sobrescribe el archivo con una cadena vacía
            }
        }



    }
}
