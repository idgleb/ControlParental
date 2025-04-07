package com.ursolgleb.controlparental.UI.adapters.marcarAppsPara

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ursolgleb.controlparental.UI.fragments.BottomSheetActualizadaFragment
import com.ursolgleb.controlparental.UI.fragments.BottomSheetMoverFragment
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.HorarioEntity
import com.ursolgleb.controlparental.databinding.ItemAppEditBinding
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
        binding.tvDiasDeLaSemana.text = horario.diasDeSemana.joinToString(", ")

        binding.itemHorarioEdit.setOnClickListener {
            Log.w("BottomSheetFragment", "onClick")
            val bottomSheetSheetMover = BottomSheetActualizadaFragment()
            bottomSheetSheetMover.show(fragmentManager, "BottomSheetDialog")

        }
    }
}

