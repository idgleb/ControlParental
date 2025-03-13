package com.ursolgleb.controlparental.UI.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.UI.adapters.blockedAppsCard.AppsCardAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.AppDataRepository
import com.ursolgleb.controlparental.data.local.dao.AppDao
import com.ursolgleb.controlparental.databinding.FragmentDisponAppsCardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DisponAppsCardFragment : Fragment(R.layout.fragment_dispon_apps_card) {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    lateinit var appDao: AppDao

    private var _binding: FragmentDisponAppsCardBinding? = null
    private val binding get() = _binding!!

    private var appCardAdapter: AppsCardAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appDao = appDataRepository.appDatabase.appDao()

        initUI(view)

        initListeners()

        initObservers()
    }

    private fun initListeners() {

        binding.aggregarAppsADisponBoton.setOnClickListener {
            val navController = Navigation.findNavController(
                requireActivity(),
                R.id.nav_host_fragment
            )
            navController.navigate(R.id.action_global_addAppsAblockedFragment)
        }

        binding.cvAppsDispon.setOnClickListener {
            navegarADisponAppsEdit()
        }
        binding.tvEmptyMessage.setOnClickListener {
            navegarADisponAppsEdit()
        }


    }

    private fun navegarADisponAppsEdit() {
        val navController = Navigation.findNavController(
            requireActivity(),
            R.id.nav_host_fragment
        )
        navController.navigate(R.id.action_global_disponAppsEditFragment)
    }

    private fun initObservers() {
        // 🔥 Observar cambios en la lista de apps dispon
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appDataRepository.disponAppsFlow.collect { newList ->
                    Log.w("BlockedAppsFragment", "Lista de apps dispon actualizada: $newList")
                    binding.tvCantidadAppsDispon.text = newList.size.toString()
                    appCardAdapter?.updateListEnAdaptador(newList.take(3))
                    // 🔥 Si la lista está vacía, mostrar "Empty"
                    if (newList.isEmpty()) {
                        binding.tvEmptyMessage.text = "No hay aplicaciones bloqueadas"
                        binding.rvAppsDispon.visibility = View.GONE
                    } else {
                        binding.tvEmptyMessage.text = ""
                        binding.rvAppsDispon.visibility = View.VISIBLE
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
                    Log.e(
                        "MioParametro",
                        "BlockedAppsCardFragment mostrarBottomSheetActualizadaFlow: $isTrue"
                    )
                    if (isTrue) {
                        // Mostrar un indicador de carga o bloquear la UI.
                        val bottomSheetActualizada = BottomSheetActualizadaFragment()
                        bottomSheetActualizada.show(parentFragmentManager, "BottomSheetDialog")
                    }
                }
            }
        }

    }

    private fun initUI(view: View) {
        _binding = FragmentDisponAppsCardBinding.bind(view)

        appCardAdapter = AppsCardAdapter(mutableListOf(), appDataRepository)
        binding.rvAppsDispon.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAppsDispon.adapter = appCardAdapter
        binding.rvAppsDispon.setRecycledViewPool(RecyclerView.RecycledViewPool()) // ✅ Optimización
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.e("BlockedAppsFragment", "onDestroyView")
        _binding = null // 🔥 Evitar memory leaks
    }
}
