package com.ursolgleb.controlparental.UI.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ursolgleb.controlparental.data.apps.entities.HorarioEntity
import com.ursolgleb.controlparental.databinding.FragmentHorarioCrearBinding
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class HorarioCrearFragment : Fragment() {

    private var _binding: FragmentHorarioCrearBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var appDataRepository: com.ursolgleb.controlparental.data.apps.AppDataRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHorarioCrearBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        initListeners()
        initObservers()
    }

    private fun initUI() {
        // Inicializar UI si es necesario
    }

    private fun initListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.crearHorarioBoton.setOnClickListener {
            val nombreHorario = binding.etNombreHorario.text.toString()
            val horaInicioStr = binding.etHoraInicio.text.toString()
            val horaFinStr = binding.etHoraFin.text.toString()
            val diasDeSemana: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7)

            if (nombreHorario.isEmpty() || horaInicioStr.isEmpty() || horaFinStr.isEmpty() || diasDeSemana.isEmpty()) {
                return@setOnClickListener
            }

            try {
                val horaInicio = LocalTime.parse(horaInicioStr)
                val horaFin = LocalTime.parse(horaFinStr)

                if (horaInicio >= horaFin) {
                    return@setOnClickListener
                }

                val horario = HorarioEntity(
                    nombreDeHorario = nombreHorario,
                    diasDeSemana = diasDeSemana,
                    horaInicio = horaInicio,
                    horaFin = horaFin,
                    isActive = true // Por defecto el horario está activo
                )
                appDataRepository.addHorarioBD(horario)
                findNavController().popBackStack()
            } catch (e: Exception) {
                // Manejar error de formato de hora
                return@setOnClickListener
            }
        }
    }

    private fun initObservers() {
        // Observadores si son necesarios
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Evitar memory leaks
    }
}
