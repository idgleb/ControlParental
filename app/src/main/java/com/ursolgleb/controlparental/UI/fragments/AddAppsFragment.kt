package com.ursolgleb.controlparental.UI.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.UI.adapters.apps.AppsAdapter
import com.ursolgleb.controlparental.UI.viewmodel.SharedViewModel
import com.ursolgleb.controlparental.databinding.FragmentAddAppsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddAppsFragment : Fragment(R.layout.fragment_add_apps) {

    private var _binding: FragmentAddAppsBinding? = null
    private val binding get() = _binding!!

    private lateinit var appAdapter: AppsAdapter
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI(view)
        initListeners()
        initObservers()
    }

    private fun initUI(view: View) {
        _binding = FragmentAddAppsBinding.bind(view)

        appAdapter = AppsAdapter(mutableListOf(), requireContext(), sharedViewModel)
        binding.rvApps.adapter = appAdapter
        binding.rvApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApps.setRecycledViewPool(RecyclerView.RecycledViewPool())
    }

    private fun initListeners() {
        binding.btnBack.setOnClickListener {
            // Usando Navigation Component para regresar:
            findNavController().popBackStack()
        }

        binding.aggregarAppsABlockedBoton.setOnClickListener {
            val selectedApps = appAdapter.getSelectedApps() // Obtener apps seleccionadas
            if (selectedApps.isNotEmpty()) {
                lifecycleScope.launch {
                    sharedViewModel.addListaStringAppsABlockedBD(selectedApps.toList())
                }
                // Regresar al fragmento anterior
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "No has seleccionado ninguna app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initObservers() {
        lifecycleScope.launch {
            sharedViewModel.todosApps.collect { apps ->
                appAdapter.updateListAppEnAdaptador(apps)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { sharedViewModel.updateBDApps() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Evitar memory leaks
    }
}
