package com.ursolgleb.controlparental.UI.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ursolgleb.controlparental.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContainerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_container)
        // Aquí no es necesario hacer nada adicional, el NavHostFragment se encarga de la navegación.
    }
}
