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
import com.ursolgleb.controlparental.UI.adapters.marcarAppsPara.HorarioEditAdapter
import com.ursolgleb.controlparental.data.local.entities.HorarioEntity
import com.ursolgleb.controlparental.databinding.FragmentAppsEditBinding
import com.ursolgleb.controlparental.databinding.FragmentHorarioCrearBinding
import com.ursolgleb.controlparental.databinding.FragmentHorarioEditBinding
import com.ursolgleb.controlparental.utils.Fun
import com.ursolgleb.controlparental.utils.StatusApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HorarioCrearFragment : Fragment(R.layout.fragment_horario_crear) {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    private var _binding: FragmentHorarioCrearBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.e("HorarioEditFragment", "onViewCreated")

        initUI(view)
        initListeners()
        initObservers()
    }

    private fun initUI(view: View) {
        _binding = FragmentHorarioCrearBinding.bind(view)

    }

    private fun initListeners() {

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.crearHorarioBoton.setOnClickListener {
            val nombreHorario = binding.etNombreHorario.text.toString()
            val horaInicio = binding.etHoraInicio.text.toString()
            val horaFin = binding.etHoraFin.text.toString()
            if (nombreHorario.isEmpty() || horaInicio.isEmpty() || horaFin.isEmpty()) {
                //Fun.mostrarToast(requireContext(), "Por favor, complete todos los campos")
                return@setOnClickListener
            }
            if (horaInicio >= horaFin) {
                //Fun.mostrarToast(requireContext(), "La hora de inicio debe ser menor a la hora de fin")
                return@setOnClickListener
            }
            val horario = HorarioEntity(nombreDeHorario = nombreHorario, diasDeSemana = listOf(), horaInicio = horaInicio, horaFin = horaFin)
            appDataRepository.addHorarioBD(horario)

            findNavController().popBackStack()
        }
    }

    private fun initObservers() {

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Evitar memory leaks
    }
}
