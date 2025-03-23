package com.ursolgleb.controlparental.UI.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
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
import com.ursolgleb.controlparental.UI.adapters.marcarAppsPara.AppsEditAdapter
import com.ursolgleb.controlparental.databinding.FragmentAppsEditBinding
import com.ursolgleb.controlparental.utils.Fun
import com.ursolgleb.controlparental.utils.StatusApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppsEditFragment : Fragment(R.layout.fragment_apps_edit) {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    private var _binding: FragmentAppsEditBinding? = null
    private val binding get() = _binding!!

    private lateinit var appsEditAdapter: AppsEditAdapter

    private val args: AppsEditFragmentArgs by navArgs() // Recibir argumentos

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI(view)
        initListeners()
        initObservers()
    }

    private fun initUI(view: View) {
        _binding = FragmentAppsEditBinding.bind(view)

        appsEditAdapter =
            AppsEditAdapter(mutableListOf(), appDataRepository, childFragmentManager)
        binding.rvAppsEdit.adapter = appsEditAdapter
        binding.rvAppsEdit.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAppsEdit.setRecycledViewPool(RecyclerView.RecycledViewPool())

        when (args.category) {
            StatusApp.BLOQUEADA.desc -> {
                binding.iconDeLista.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.lock, null)
            }

            StatusApp.DISPONIBLE.desc -> {
                binding.iconDeLista.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.vecteezy_infinity, null)
                val params = binding.iconDeLista.layoutParams
                params.width = Fun.dpToPx(26, binding.iconDeLista)
                params.height = Fun.dpToPx(12, binding.iconDeLista)
                binding.iconDeLista.layoutParams = params
            }

            StatusApp.HORARIO.desc -> {
                binding.iconDeLista.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.vect_clock_timer, null)
                val params = binding.iconDeLista.layoutParams
                params.width = Fun.dpToPx(43, binding.iconDeLista)
                params.height = Fun.dpToPx(95, binding.iconDeLista)
                binding.iconDeLista.layoutParams = params
            }

            else -> {
                binding.iconDeLista.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_check_white, null)
            }

        }

        binding.tvNombreLista.text = when (args.category) {
            StatusApp.BLOQUEADA.desc -> "Bloqueadas siempre"
            StatusApp.DISPONIBLE.desc -> "Disponibles siempre"
            StatusApp.HORARIO.desc -> "Bajo de Horario"
            else -> "Apps ..."
        }
        binding.aggregarAppsABoton.text = when (args.category) {
            StatusApp.BLOQUEADA.desc -> "Agregar a siempre bloqueadas"
            StatusApp.DISPONIBLE.desc -> "Agregar a siempre disponibles"
            StatusApp.HORARIO.desc -> "Agregar a bajo horario"
            else -> "Agregar a ..."
        }

    }

    private fun initListeners() {

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.aggregarAppsABoton.setOnClickListener {
            val action =
                MainAdminFragmentDirections.actionGlobalAddAppsA(category = args.category)
            findNavController().navigate(action)
        }
    }

    private fun initObservers() {

        val listaDeObservarFlow = when (args.category) {
            StatusApp.BLOQUEADA.desc -> appDataRepository.blockedAppsFlow
            StatusApp.DISPONIBLE.desc -> appDataRepository.disponAppsFlow
            StatusApp.HORARIO.desc -> appDataRepository.horarioAppsFlow
            else -> appDataRepository.todosAppsFlow
        }

        // 🔥 Observar cambios en la lista de apps bloqueadas
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                listaDeObservarFlow.collect { newList ->
                    Log.w(
                        "AppsEditFragment",
                        "Lista EDIT de apps ${args.category} actualizada: $newList"
                    )
                    binding.tvCantidadApps.text = newList.size.toString()
                    appsEditAdapter.updateListEnAdaptador(newList)
                    // 🔥 Si la lista está vacía, mostrar "Empty"
                    binding.tvEmptyMessage.text = if (newList.isEmpty()) {
                         "No hay aplicaciones ${args.category}"
                    } else ""

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
