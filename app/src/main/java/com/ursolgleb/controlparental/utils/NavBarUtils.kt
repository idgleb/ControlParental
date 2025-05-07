package com.ursolgleb.controlparental.utils


import android.app.Activity
import android.app.Dialog
import android.os.Build
import android.view.Window
import android.view.WindowInsetsController

object NavBarUtils {

    fun aplicarEstiloNavBar(source: Any?) {

        val window: Window? = when (source) {
            is Activity -> source.window
            is Dialog -> source.window
            is Window -> source
            else -> null
        }

        if (window == null) return // Evita errores si `window` es null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { // Android 11+
            window.isNavigationBarContrastEnforced = false // ✅ Evita contraste forzado
            window.navigationBarColor =
                window.context.getColor(android.R.color.black) // 🔥 Fondo negro
            window.insetsController?.setSystemBarsAppearance(
                0, // 🔥 Remueve cualquier apariencia previa
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {// Android 8.1 - 10
            @Suppress("DEPRECATION")
            window.navigationBarColor =
                window.context.getColor(android.R.color.black) // ✅ Fondo negro
            window.decorView.systemUiVisibility = 0 // ✅ Asegurar iconos blancos
        }


    }


}
