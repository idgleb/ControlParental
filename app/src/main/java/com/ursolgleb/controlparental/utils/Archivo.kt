package com.ursolgleb.controlparental.utils

import android.content.Context
import java.io.File

class Archivo {
    companion object {
        fun appendTextToFile(context: Context, fileName: String, text: String) {
            val file = File(context.filesDir, fileName)
            file.appendText(text) // Agrega texto sin borrar lo anterior
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
