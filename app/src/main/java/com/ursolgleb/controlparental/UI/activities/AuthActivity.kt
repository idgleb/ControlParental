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
import com.ursolgleb.controlparental.validadors.PinValidator
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    @Inject lateinit var pinValidator: PinValidator

    private var pinDialog: AlertDialog? = null      // ahora coincide con el builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(false)          // evita cerrar tocando fuera
        launchBiometricPrompt()
    }

    /** ---------- BIOMETRÍA ---------- **/
    private fun launchBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(r: BiometricPrompt.AuthenticationResult) {
                    sendResultAndFinish(true)
                }

                override fun onAuthenticationError(code: Int, msg: CharSequence) {
                    // -10 cancel, -13 no enrolado → fallback a PIN
                    showPinFallback()
                }
            })

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Control parental")
            .setSubtitle("Identifícate para continuar")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Usar PIN")          // <- NUEVO
            .build()


        prompt.authenticate(info)
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
            .setTitle("Introduce tu PIN")
            .setCancelable(false)
            .setView(input)
            .setPositiveButton("Aceptar") { _, _ ->
                if (pinValidator.isPinCorrect(input.text.toString())) {
                    sendResultAndFinish(true)
                } else {
                    //Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                    //showPinFallback()                 // vuelve a mostrarlo
                }
            }
            .setNegativeButton("Cancelar") { _, _ ->
                sendResultAndFinish(false)
            }
            .show()
    }

    /* ---------- Devuelve el resultado ---------- */
    private fun sendResultAndFinish(ok: Boolean) {
        // cierra diálogo si sigue abierto antes de terminar
        pinDialog?.dismiss()
        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent("AUTH_RESULT").putExtra("ok", ok))
        finish()
    }

    /* ---------- Limpieza ---------- */
    override fun onDestroy() {
        pinDialog?.dismiss()       // <-- evita WindowLeaked
        super.onDestroy()
    }
}
