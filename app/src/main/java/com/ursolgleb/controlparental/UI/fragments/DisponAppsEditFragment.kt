package com.ursolgleb.controlparental.UI.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.AppDataRepository
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.UI.adapters.marcarAppsParaBlockear.appsEditAdapter
import com.ursolgleb.controlparental.databinding.FragmentDisponAppsEditBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DisponAppsEditFragment : Fragment(R.layout.fragment_dispon_apps_edit) {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    private var _binding: FragmentDisponAppsEditBinding? = null
    private val binding get() = _binding!!

    private lateinit var appsEditAdapter: appsEditAdapter
    //private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI(view)
        initListeners()
        initObservers()
    }

    private fun initUI(view: View) {
        _binding = FragmentDisponAppsEditBinding.bind(view)

        appsEditAdapter =
            appsEditAdapter(mutableListOf(), appDataRepository, childFragmentManager)
        binding.rvBlockedAppsEdit.adapter = appsEditAdapter
        binding.rvBlockedAppsEdit.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBlockedAppsEdit.setRecycledViewPool(RecyclerView.RecycledViewPool())

    }

    private fun initListeners() {

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.aggregarAppsADisponBoton.setOnClickListener {
            val navController = Navigation.findNavController(
                requireActivity(),
                R.id.nav_host_fragment
            )
            navController.navigate(R.id.action_global_addAppsAblockedFragment)
        }
    }

    private fun initObservers() {

        // 🔥 Observar cambios en la lista de apps bloqueadas
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appDataRepository.disponAppsFlow.collect { newList ->
                    Log.w(
                        "DisponAppsEditFragment",
                        "Lista EDIT de apps dispon actualizada: $newList"
                    )
                    binding.tvCantidadAppsDispon.text = newList.size.toString()
                    appsEditAdapter.updateListEnAdaptador(newList)
                    // 🔥 Si la lista está vacía, mostrar "Empty"
                    if (newList.isEmpty()) {
                        binding.tvEmptyMessage.visibility = View.VISIBLE
                        binding.rvBlockedAppsEdit.visibility = View.GONE
                    } else {
                        binding.tvEmptyMessage.visibility = View.GONE
                        binding.rvBlockedAppsEdit.visibility = View.VISIBLE
                    }
                }
            }
        }

        // 🔥 Observar si updateBDApps() esta en processo
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appDataRepository.mutexUpdateBDAppsStateFlow.collect { isLocked ->
                    // Aquí se actualiza cada vez que cambia el estado del mutex.
                    if (isLocked) {
                        // Mostrar un indicador de carga o bloquear la UI.
                        Log.d("UI", "La operación updateBDApps está en curso")
                        binding.progressBarUpdateBD.visibility = View.VISIBLE
                    } else {
                        // Ocultar el indicador de carga o desbloquear la UI.
                        Log.d("UI", "La operación updateBDApps ha finalizado")
                        binding.progressBarUpdateBD.visibility = View.GONE
                    }
                }
            }
        }

        // 🔥 Observar si se necesita mostrar el mostrarBottomSheetActualizada
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appDataRepository.mostrarBottomSheetActualizadaFlow.collect { isTrue ->
                    if (isTrue) {
                        // Mostrar un indicador de carga o bloquear la UI.
                        val bottomSheetActualizada = BottomSheetActualizadaFragment()
                        bottomSheetActualizada.show(parentFragmentManager, "BottomSheetDialog")
                    }
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Evitar memory leaks
    }
}
