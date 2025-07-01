package com.ursolgleb.controlparental.UI.activities

import android.os.Bundle
import android.os.Build
import androidx.annotation.RequiresApi
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.ursolgleb.controlparental.data.local.AppDataRepository
import com.ursolgleb.controlparental.utils.Session
import com.ursolgleb.controlparental.validadors.PinValidator
import com.ursolgleb.controlparental.databinding.ActivityAuthBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    @Inject
    lateinit var pinValidator: PinValidator

    @Inject
    lateinit var appDataRepository: AppDataRepository

    @Inject
    lateinit var session: Session

    private lateinit var binding: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initListeners()
    }

    override fun onResume() {
        super.onResume()
        pinValidator.setAuthActivitiAbierta(true)
    }

    override fun onPause() {
        super.onPause()
        pinValidator.setAuthActivitiAbierta(false)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        pinValidator.setAuthActivitiAbierta(hasFocus)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onTopResumedActivityChanged(isTopResumed: Boolean) {
        super.onTopResumedActivityChanged(isTopResumed)
        pinValidator.setAuthActivitiAbierta(isTopResumed)
    }

    private fun initListeners() {
        val digitButtons: List<Pair<Button, String>> = listOf(
            binding.btnNum0 to "0",
            binding.btnNum1 to "1",
            binding.btnNum2 to "2",
            binding.btnNum3 to "3",
            binding.btnNum4 to "4",
            binding.btnNum5 to "5",
            binding.btnNum6 to "6",
            binding.btnNum7 to "7",
            binding.btnNum8 to "8",
            binding.btnNum9 to "9",
        )
        digitButtons.forEach { (button, digit) ->
            button.setOnClickListener { appendDigit(digit) }
        }
        binding.btnAceptar.setOnClickListener { validarPin() }
        binding.btnCancelar.setOnClickListener {
            session.cerrarSesion()
            finish()
        }
    }

    private fun appendDigit(digit: String) {
        binding.etPin.append(digit)
    }


    private fun validarPin() {
        val pin = binding.etPin.text.toString()
        if (pinValidator.isPinCorrect(pin)) {
            session.iniciarSesion()
            finish()
        } else {
            Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
            session.cerrarSesion()
            binding.etPin.text?.clear()
        }
    }
}