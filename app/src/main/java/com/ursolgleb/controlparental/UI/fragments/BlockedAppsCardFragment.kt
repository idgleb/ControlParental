package com.ursolgleb.controlparental.UI.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.UI.adapters.blockedAppsCard.BlockedAppsCardAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.AppDataRepository
import com.ursolgleb.controlparental.data.local.dao.AppDao
import com.ursolgleb.controlparental.databinding.FragmentBlockedAppsCardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class BlockedAppsCardFragment : Fragment(R.layout.fragment_blocked_apps_card) {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    lateinit var appDao: AppDao

    private var _binding: FragmentBlockedAppsCardBinding? = null
    private val binding get() = _binding!!

    private var blockedAppCardAdapter: BlockedAppsCardAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appDao = appDataRepository.appDatabase.appDao()

        initUI(view)

        initListeners()

        initObservers()
    }

    private fun initListeners() {

        binding.aggregarAppsABlockedBoton.setOnClickListener {
            val navController = Navigation.findNavController(
                requireActivity(),
                R.id.nav_host_fragment
            )
            navController.navigate(R.id.action_global_addAppsAblockedFragment)
        }

        binding.delitBlackedAppBoton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                appDao.unblockAllApps()
                withContext(Dispatchers.Main) {
                    Log.w("BlockedAppsFragment", "Apps delited")
                }
            }
        }

        binding.cvAppsBlocked.setOnClickListener {
            navegarABlockedAppsEdit()
        }
        binding.tvEmptyMessage.setOnClickListener {
            navegarABlockedAppsEdit()
        }

        binding.delitAllAppBoton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                appDao.deleteAllApps()
                withContext(Dispatchers.Main) {
                    Log.e("BlockedAppsFragment", "Todos Apps delited")
                }
            }
        }

        binding.llenarAllAppBoton.setOnClickListener {
            appDataRepository.updateBDApps()
        }

        binding.testBoton.setOnClickListener {
/*            val pkgName = "com.ursolgleb.controlparental"
            Log.e("MioParametro", "testBoton")
            val listPkgName = listOf(pkgName)
            val tiempoDeUso = appDataRepository.getTiempoDeUsoSeconds(listPkgName) { app -> app }
            Log.e("MioParametro", "getTiempoDeUsoSeconds $pkgName: $tiempoDeUso")*/

            appDataRepository.getUsageStats30dias2()

        }

    }

    private fun navegarABlockedAppsEdit() {
        val navController = Navigation.findNavController(
            requireActivity(),
            R.id.nav_host_fragment
        )
        navController.navigate(R.id.action_global_blockedAppsEditFragment)
    }

    private fun initObservers() {
        // 🔥 Observar cambios en la lista de apps bloqueadas
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appDataRepository.blockedAppsFlow.collect { newList ->
                    Log.w("BlockedAppsFragment", "Lista de apps bloqueadas actualizada: $newList")
                    binding.tvCantidadAppsBloqueadas.text = newList.size.toString()
                    blockedAppCardAdapter?.updateListEnAdaptador(newList.take(3))
                    // 🔥 Si la lista está vacía, mostrar "Empty"
                    if (newList.isEmpty()) {
                        binding.tvEmptyMessage.text = "No hay aplicaciones bloqueadas"
                        binding.rvAppsBloqueadas.visibility = View.GONE
                    } else {
                        binding.tvEmptyMessage.text = ""
                        binding.rvAppsBloqueadas.visibility = View.VISIBLE
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
        _binding = FragmentBlockedAppsCardBinding.bind(view)

        blockedAppCardAdapter = BlockedAppsCardAdapter(mutableListOf(), appDataRepository)
        binding.rvAppsBloqueadas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAppsBloqueadas.adapter = blockedAppCardAdapter
        binding.rvAppsBloqueadas.setRecycledViewPool(RecyclerView.RecycledViewPool()) // ✅ Optimización
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.e("BlockedAppsFragment", "onDestroyView")
        _binding = null // 🔥 Evitar memory leaks
    }
}
