package com.ursolgleb.controlparental.UI.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.data.apps.entities.HorarioEntity
import com.ursolgleb.controlparental.databinding.FragmentHorarioCrearBinding
import dagger.hilt.android.AndroidEntryPoint
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
