package com.ursolgleb.controlparental.utils

import android.content.Context
import android.os.FileObserver
import java.io.File

class FileWatcher(private val file: File, private val onChange: (String) -> Unit) :
    FileObserver(file.absolutePath, MODIFY) {

    override fun onEvent(event: Int, path: String?) {
        if (event == MODIFY) {
            onChange(file.readText()) // Llama al callback con el contenido actualizado
        }
    }

    companion object{
        fun observeFileChanges(context: Context, fileName: String, onChange: (String) -> Unit): FileWatcher {
            val file = File(context.filesDir, fileName)
            // Asegurar que el archivo existe
            if (!file.exists()) file.createNewFile()
            val watcher = FileWatcher(file, onChange)
            watcher.startWatching()
            return watcher // Retorna el objeto para detenerlo si es necesario
        }
    }


}
