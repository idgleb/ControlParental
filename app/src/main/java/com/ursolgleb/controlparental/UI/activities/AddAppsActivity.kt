package com.ursolgleb.controlparental.UI.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.UI.adapters.apps.AppsAdapter
import com.ursolgleb.controlparental.UI.viewmodel.SharedViewModel
import com.ursolgleb.controlparental.databinding.ActivityAddAppsBinding
import kotlinx.coroutines.launch

class AddAppsActivity : AppCompatActivity() {
    lateinit var appAdapter: AppsAdapter
    lateinit var bindAddApps: ActivityAddAppsBinding

    private val sharedViewModel: SharedViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        bindAddApps = ActivityAddAppsBinding.inflate(layoutInflater)
        setContentView(bindAddApps.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_add_apps)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initUI()

        initListeners()

        initObservers()

    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { sharedViewModel.updateBDApps() }
    }

    private fun initListeners() {
        bindAddApps.btnBack.setOnClickListener {
            finish()
        }

        bindAddApps.aggregarAppsABlockedBoton.setOnClickListener {
            val selectedApps = appAdapter.getSelectedApps() // 🔥 Obtener apps seleccionadas
            if (selectedApps.isNotEmpty()) {
                bloquearAppsSeleccionadas(selectedApps)
                finish()
            } else {
                Toast.makeText(this, "No has seleccionado ninguna app", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun bloquearAppsSeleccionadas(selectedApps: Set<String>) {
        for (app in selectedApps) {
            sharedViewModel.addAppBlockListBD(app)
        }
    }

    private fun initObservers() {
        // 🔥 Observar cambios en la lista de apps
        lifecycleScope.launch {
            sharedViewModel.todosApps.collect { apps ->
                appAdapter.updateListAppEnAdaptador(apps)
            }
        }
    }

    private fun initUI() {

        appAdapter = AppsAdapter(mutableListOf(), this, sharedViewModel)
        bindAddApps.rvApps.adapter = appAdapter
        bindAddApps.rvApps.layoutManager = LinearLayoutManager(this)
        bindAddApps.rvApps.setRecycledViewPool(RecyclerView.RecycledViewPool())



        //
    }
}