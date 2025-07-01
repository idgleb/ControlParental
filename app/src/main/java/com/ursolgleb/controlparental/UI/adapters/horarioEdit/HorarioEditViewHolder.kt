package com.ursolgleb.controlparental.UI.adapters.marcarAppsPara

import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.UI.fragments.MainAdminFragmentDirections
import com.ursolgleb.controlparental.data.local.AppDataRepository
import com.ursolgleb.controlparental.data.local.entities.HorarioEntity
import com.ursolgleb.controlparental.databinding.ItemHorarioEditBinding
import androidx.navigation.findNavController
import com.ursolgleb.controlparental.handlers.SyncHandler
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class HorarioEditViewHolder(
    private val binding: ItemHorarioEditBinding,
    private val fragmentManager: androidx.fragment.app.FragmentManager,
    val appDataRepository: AppDataRepository,
    val syncHendler: SyncHandler
) : RecyclerView.ViewHolder(binding.root) {


    fun bind(
        horario: HorarioEntity
    ) {
        binding.tvHorarioName.text = horario.nombreDeHorario
        binding.tvInicioFin.text = "${horario.horaInicio} - ${horario.horaFin}"

        val diasMap = mapOf(
            1 to "Lu",
            2 to "Ma",
            3 to "Mi",
            4 to "Ju",
            5 to "Vi",
            6 to "Sa",
            7 to "Do"
        )
        binding.tvDiasDeLaSemana.text = horario.diasDeSemana
            .mapNotNull { diasMap[it] }
            .joinToString(", ")

        binding.switchHorario.isChecked = horario.isActive

        // Configurar el listener para el switch
        binding.switchHorario.setOnCheckedChangeListener { _, isChecked ->
            val horarioActualizado = horario.copy(isActive = isChecked)
            CoroutineScope(Dispatchers.Main).launch {
                appDataRepository.addHorarioBD(horarioActualizado)
            }
        }

        binding.itemHorarioEdit.setOnClickListener {
            val action = MainAdminFragmentDirections.actionGlobalHorarioCrearFragment(horario)
            binding.root.findNavController().navigate(action)
        }
    }
}

