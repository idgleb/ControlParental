package com.ursolgleb.controlparental.UI.adapters.marcarAppsPara

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.UI.fragments.BottomSheetActualizadaFragment
import com.ursolgleb.controlparental.data.apps.entities.HorarioEntity
import com.ursolgleb.controlparental.databinding.ItemHorarioEditBinding

class HorarioEditViewHolder(
    private val binding: ItemHorarioEditBinding,
    private val fragmentManager: androidx.fragment.app.FragmentManager
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


        binding.itemHorarioEdit.setOnClickListener {
            Log.w("BottomSheetFragment", "onClick")
            val bottomSheetSheetMover = BottomSheetActualizadaFragment()
            bottomSheetSheetMover.show(fragmentManager, "BottomSheetDialog")

        }
    }
}

