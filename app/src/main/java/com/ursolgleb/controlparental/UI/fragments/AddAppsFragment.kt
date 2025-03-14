package com.ursolgleb.controlparental.UI.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.AppDataRepository
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.UI.adapters.marcarAppsParaBlockear.MarcarAppsParaAgregarAdapter
import com.ursolgleb.controlparental.databinding.FragmentAddAppsBinding
import com.ursolgleb.controlparental.utils.StatusApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AddAppsFragment : Fragment(R.layout.fragment_add_apps) {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    private var _binding: FragmentAddAppsBinding? = null
    private val binding get() = _binding!!

    private lateinit var marcarAppsParaAgregarAdapter: MarcarAppsParaAgregarAdapter

    private val args: AddAppsFragmentArgs by navArgs() // Recibir argumentos

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        args.category

        initUI(view)
        initListeners()
        initObservers()
    }

    private fun initUI(view: View) {
        _binding = FragmentAddAppsBinding.bind(view)

        marcarAppsParaAgregarAdapter =
            MarcarAppsParaAgregarAdapter(mutableListOf(), appDataRepository)
        binding.rvMarcarAppsParaBloquear.adapter = marcarAppsParaAgregarAdapter
        binding.rvMarcarAppsParaBloquear.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMarcarAppsParaBloquear.setRecycledViewPool(RecyclerView.RecycledViewPool())
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun initListeners() {

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.aggregarAppsABlockedBoton.setOnClickListener {
            val selectedApps =
                marcarAppsParaAgregarAdapter.getSelectedApps() // Obtener apps seleccionadas
            if (selectedApps.isNotEmpty()) {
                GlobalScope.launch {
                    if (args.category == StatusApp.DISPONIBLE.desc) {
                        appDataRepository.addAppsASiempreDisponiblesBD(selectedApps.toList())
                    }
                    if (args.category == StatusApp.BLOQUEADA.desc) {
                        appDataRepository.addAppsASiempreBloqueadasBD(selectedApps.toList())
                    }

                }
                findNavController().popBackStack()
            } else {
                Toast.makeText(
                    requireContext(),
                    "No has seleccionado ninguna app",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun initObservers() {

        val listaDeObservarFlow = when (args.category) {
            StatusApp.BLOQUEADA.desc -> appDataRepository.todosAppsMenosBloqueadosFlow
            StatusApp.DISPONIBLE.desc -> appDataRepository.todosAppsMenosDisponFlow
            StatusApp.HORARIO.desc -> appDataRepository.todosAppsMenosHorarioFlow
            else -> appDataRepository.todosAppsFlow
        }

        // 🔥 Observar cambios en la lista de todosAppsMenosBlaqueados
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                listaDeObservarFlow.collect { newList ->
                    Log.w("BlockedAppsFragment", "Lista de apps actualizada: $newList")
                    marcarAppsParaAgregarAdapter.updateListEnAdaptador(newList)
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
