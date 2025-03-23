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
import com.ursolgleb.controlparental.UI.adapters.marcarAppsPara.MarcarAppsParaAgregarAdapter
import com.ursolgleb.controlparental.databinding.FragmentAddAppsBinding
import com.ursolgleb.controlparental.utils.StatusApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppsAddFragment : Fragment(R.layout.fragment_add_apps) {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    private var _binding: FragmentAddAppsBinding? = null
    private val binding get() = _binding!!

    private lateinit var marcarAppsParaAgregarAdapter: MarcarAppsParaAgregarAdapter

    private val args: AppsAddFragmentArgs by navArgs() // Recibir argumentos

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        if (args.category == StatusApp.BLOQUEADA.desc) {

        }
        binding.tvTitulo.text = when (args.category) {
            StatusApp.BLOQUEADA.desc -> "Marca las apps que deseas bloquear"
            StatusApp.DISPONIBLE.desc -> "Marca las apps para sean siempre disponibles"
            StatusApp.HORARIO.desc -> "Marca las apps para sean bajo del horario."
            else -> {
                "No hay categorias"
            }
        }
        binding.aggregarAppsABoton.text = when (args.category) {
            StatusApp.BLOQUEADA.desc -> "Agregar a siempre blockeadas"
            StatusApp.DISPONIBLE.desc -> "Agregar a siempre disponibles"
            StatusApp.HORARIO.desc -> "Agregar a bajo horario"
            else -> {
                "No hay categorias"
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun initListeners() {

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.aggregarAppsABoton.setOnClickListener {
            val selectedApps =
                marcarAppsParaAgregarAdapter.getSelectedApps() // Obtener apps seleccionadas
            if (selectedApps.isNotEmpty()) {
                GlobalScope.launch {
                    when (args.category) {
                        StatusApp.BLOQUEADA.desc ->
                            appDataRepository.addAppsASiempreBloqueadasBD(selectedApps.toList())

                        StatusApp.DISPONIBLE.desc ->
                            appDataRepository.addAppsASiempreDisponiblesBD(selectedApps.toList())

                        StatusApp.HORARIO.desc -> appDataRepository.addAppsAHorarioBD(
                            selectedApps.toList()
                        )
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
