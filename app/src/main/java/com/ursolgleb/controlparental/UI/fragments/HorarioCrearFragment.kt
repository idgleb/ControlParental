package com.ursolgleb.controlparental.UI.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.chip.Chip
import com.ursolgleb.controlparental.data.apps.entities.HorarioEntity
import com.ursolgleb.controlparental.databinding.FragmentHorarioCrearBinding
import com.ursolgleb.controlparental.handlers.SyncHandler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class HorarioCrearFragment : Fragment() {

    private val args: HorarioCrearFragmentArgs by navArgs()
    private lateinit var horario: HorarioEntity

    private var _binding: FragmentHorarioCrearBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var appDataRepository: com.ursolgleb.controlparental.data.apps.AppDataRepository

    @Inject
    lateinit var syncHendler: SyncHandler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHorarioCrearBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        horario = args.horario

        initUI()
        initListeners()
        initObservers()
    }

    private fun initUI() {

        if (horario.idHorario != 0L) {
            binding.tvTituloDeFragment.text = "Editar horario de bloqueo"
            binding.btnEliminarHorario.visibility = View.VISIBLE
        }
        binding.tpHoraInicio.setIs24HourView(true)
        binding.tpHoraFin.setIs24HourView(true)

        binding.etNombreHorario.setText(horario.nombreDeHorario)

        val inicio = LocalTime.parse(horario.horaInicio)
        binding.tpHoraInicio.apply {
            hour = inicio.hour
            minute = inicio.minute
        }
        val fin = LocalTime.parse(horario.horaFin)
        binding.tpHoraFin.apply {
            hour = fin.hour
            minute = fin.minute
        }


        val diasActivos = horario.diasDeSemana.toSet()
        binding.chipGroupDias.apply {
            for (i in 0 until childCount) {
                val chip = getChildAt(i) as Chip
                val numeroDia = i + 1
                chip.isChecked = numeroDia in diasActivos // marca si corresponde
            }
        }


    }

    private fun initListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnEliminarHorario.setOnClickListener {
            appDataRepository.deleteHorarioBD(horario)
            syncHendler.setPushHorarioPendiente(true)
            findNavController().popBackStack()
        }

        binding.crearHorarioBoton.setOnClickListener {
            val nombreHorario = binding.etNombreHorario.text.toString()

            val horaInicio = binding.tpHoraInicio.hour
            val minutoInicio = binding.tpHoraInicio.minute
            val horaInicioStr = String.format(Locale.ROOT, "%02d:%02d", horaInicio, minutoInicio)

            val horaFin = binding.tpHoraFin.hour
            val minutoFin = binding.tpHoraFin.minute
            val horaFinStr = String.format(Locale.ROOT, "%02d:%02d", horaFin, minutoFin)

            val diasDeSemana: List<Int> =
                binding.chipGroupDias.checkedChipIds.map { id ->
                    val chipView = binding.chipGroupDias.findViewById<Chip>(id)
                    binding.chipGroupDias.indexOfChild(chipView) + 1
                }.sorted()

            if (nombreHorario.isEmpty() || horaInicioStr.isEmpty() || horaFinStr.isEmpty() || diasDeSemana.isEmpty()) {
                return@setOnClickListener
            }

            try {
                val horaInicio = LocalTime.parse(horaInicioStr)
                val horaFin = LocalTime.parse(horaFinStr)

                if (horaInicio >= horaFin) {
                    return@setOnClickListener
                }

                appDataRepository.scope.launch {
                    // Si es un horario nuevo (idHorario == 0), generar un nuevo ID
                    val finalIdHorario = if (horario.idHorario == 0L) {
                        // Obtener el máximo idHorario actual y sumar 1
                        val maxId = appDataRepository.horariosFlow.value
                            .filter { it.deviceId == appDataRepository.getOrCreateDeviceId() }
                            .maxOfOrNull { it.idHorario } ?: 0L
                        maxId + 1
                    } else {
                        horario.idHorario
                    }

                    val nuevoHorario = HorarioEntity(
                        idHorario = finalIdHorario,
                        deviceId = appDataRepository.getOrCreateDeviceId(),
                        nombreDeHorario = nombreHorario,
                        diasDeSemana = diasDeSemana,
                        horaInicio = horaInicio.toString(),
                        horaFin = horaFin.toString(),
                        isActive = horario.isActive
                    )

                    appDataRepository.addHorarioBD(nuevoHorario)
                    syncHendler.setPushHorarioPendiente(true)
                    
                    requireActivity().runOnUiThread {
                        findNavController().popBackStack()
                    }
                }
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
