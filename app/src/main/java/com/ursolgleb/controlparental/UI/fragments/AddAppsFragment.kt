package com.ursolgleb.controlparental.UI.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
            findNavController().popBackStack()
        }

        binding.aggregarAppsABlockedBoton.setOnClickListener {
            val selectedApps = appAdapter.getSelectedApps() // Obtener apps seleccionadas
            if (selectedApps.isNotEmpty()) {
                lifecycleScope.launch {
                    sharedViewModel.addListaStringAppsABlockedBD(selectedApps.toList())
                }
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "No has seleccionado ninguna app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initObservers() {

        // 🔥 Observar cambios en la lista de apps bloqueadas
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.todosAppsMenosBlaqueados.collect { newList ->
                    Log.w("BlockedAppsFragment", "Lista de apps actualizada: $newList")
                    appAdapter.updateListAppEnAdaptador(newList)
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Evitar memory leaks
    }
}
