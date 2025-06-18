package com.ursolgleb.controlparental.UI.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.UI.adapters.marcarAppsPara.HorarioEditAdapter
import com.ursolgleb.controlparental.data.apps.entities.HorarioEntity
import com.ursolgleb.controlparental.databinding.FragmentHorarioEditBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class HorarioEditFragment : Fragment(R.layout.fragment_horario_edit) {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    private var _binding: FragmentHorarioEditBinding? = null
    private val binding get() = _binding!!

    private lateinit var HorarioEditAdapter: HorarioEditAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.e("HorarioEditFragment", "onViewCreated")

        initUI(view)
        initListeners()
        initObservers()
    }

    private fun initUI(view: View) {
        _binding = FragmentHorarioEditBinding.bind(view)

        HorarioEditAdapter =
            HorarioEditAdapter(mutableListOf(), appDataRepository, childFragmentManager)
        binding.rvHorariosEdit.adapter = HorarioEditAdapter
        binding.rvHorariosEdit.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHorariosEdit.setRecycledViewPool(RecyclerView.RecycledViewPool())

    }

    private fun initListeners() {

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.aggregarHorariosABoton.setOnClickListener {
            val horario = HorarioEntity(
                deviceId = appDataRepository.getOrCreateDeviceId(),
                nombreDeHorario = "",
                diasDeSemana = listOf(),
                horaInicio = LocalTime.of(0, 0),
                horaFin = LocalTime.of(0, 0),
                isActive = true
            )
            val action = MainAdminFragmentDirections.actionGlobalHorarioCrearFragment(horario)
            findNavController().navigate(action)
        }
    }

    private fun initObservers() {

        //  Observar cambios en la lista de apps bloqueadas
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appDataRepository.horariosFlow.collect { newList ->
                    HorarioEditAdapter.updateListEnAdaptador(newList)
                    //  Si la lista está vacía, mostrar "Empty"
                    binding.tvEmptyMessage.text = if (newList.isEmpty()) {
                         "No hay horarios"
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
