package com.ursolgleb.controlparental.UI.activities

import android.os.Build
import android.os.Bundle
import android.view.WindowInsetsController
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.utils.NavBarUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ContainerActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_container)

        NavBarUtils.aplicarEstiloNavBar(this)


    }



}