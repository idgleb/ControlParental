package com.ursolgleb.controlparental.UI.activities

import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.work.WorkManager
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.utils.NavBarUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ContainerActivity : BaseAuthActivity() {

    @Inject
    lateinit var workManager: WorkManager
    
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // BaseAuthActivity ya verifica la autenticación en onCreate
        // Si llegamos aquí, significa que hay credenciales válidas
        
        // Mostrar el contenido normal de la aplicación
        setContentView(R.layout.activity_container)
        NavBarUtils.aplicarEstiloNavBar(this)
        
        // Log para debugging
        android.util.Log.d("ContainerActivity", "onCreate: Mostrando contenido principal")
    }
}