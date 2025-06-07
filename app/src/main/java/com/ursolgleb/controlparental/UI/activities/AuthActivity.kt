package com.ursolgleb.controlparental.UI.activities

import androidx.appcompat.app.AlertDialog
import android.content.Intent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.utils.Session
import com.ursolgleb.controlparental.validadors.PinValidator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    @Inject lateinit var pinValidator: PinValidator

    @Inject lateinit var session: Session

    private var pinDialog: AlertDialog? = null      // ahora coincide con el builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)          // evita cerrar tocando fuera
        showPinFallback()
    }

    /* ---------- PIN fallback ---------- */
    private fun showPinFallback() {

        if (pinDialog?.isShowing == true) return   // ya hay un diálogo activo
        if (isFinishing || isDestroyed) return     // evita BadTokenException


        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_VARIATION_PASSWORD
        }

        pinDialog = MaterialAlertDialogBuilder(this)
            .setTitle("PIN")
            .setCancelable(false)
            .setView(input)
            .setPositiveButton("Aceptar") { _, _ ->
                if (pinValidator.isPinCorrect(input.text.toString())) {
                    session.iniciarSesion()
                    finish()
                } else {
                    Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                    session.cerrarSesion()
                    finish()
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                session.cerrarSesion()
                finish()
            }
            .show()
    }


    /* ---------- Limpieza ---------- */
    override fun onDestroy() {
        pinDialog?.dismiss()       // <-- evita WindowLeaked
        super.onDestroy()
    }
}
