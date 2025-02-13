package com.ursolgleb.controlparental.UI.activities

import android.content.Intent
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.UI.fragments.BlockedAppsFragment
import com.ursolgleb.controlparental.UI.viewmodel.SharedViewModel
import com.ursolgleb.controlparental.databinding.ActivityAdminMainBinding
import kotlinx.coroutines.launch

class MainAdminActivity : AppCompatActivity() {
    private val sharedViewModel: SharedViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }
    lateinit var bindAdminMain: ActivityAdminMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        bindAdminMain = ActivityAdminMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(bindAdminMain.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_admin)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initUI()

        initListeners()

        initObservadores()

    }

    private fun initUI() {

        initHeightDeSvInfo()

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_apps_bloqueadas, BlockedAppsFragment()) // ✅ Cargar el Fragment
            .commit()

    }

    private fun initListeners() {
        bindAdminMain.ayudaBoton.setOnClickListener {
            intent = Intent(this, DesarolloActivity::class.java)
            startActivity(intent)
        }

    }

    private fun initObservadores() {

    }

    private fun initHeightDeSvInfo() {
        bindAdminMain.svInfo.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                bindAdminMain.svInfo.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val scrollViewHeight = bindAdminMain.svInfo.height
                // Calcula el 50% y lo aplica a vFondo
                val newHeight = (scrollViewHeight * 0.5).toInt()
                val params = bindAdminMain.vFondo.layoutParams
                params.height = newHeight
                bindAdminMain.vFondo.layoutParams = params
            }
        })
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { sharedViewModel.updateBDApps() }
        //lifecycleScope.launch { sharedViewModel.loadBlockedAppsDeBDaViewModel() }
    }


}