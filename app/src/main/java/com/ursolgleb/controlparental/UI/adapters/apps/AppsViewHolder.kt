package com.ursolgleb.controlparental.UI.adapters.apps

import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ursolgleb.controlparental.databinding.ItemAppGrandeBinding
import com.ursolgleb.controlparental.databinding.ItemBlockedAppBinding

class AppsViewHolder(private val binding: ItemAppGrandeBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(appName: String?, icon: Drawable?, horasDeUso: Double, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        binding.tvAppName.text = appName ?: "No encontrada"
        Glide.with(binding.ivIconoApp.context)
            .load(icon) // Glide acepta `Drawable` y `Bitmap`
            .into(binding.ivIconoApp)
        binding.tvHorasDeUso.text = "Horas de uso: $horasDeUso"

        binding.cbApp.setOnCheckedChangeListener(null) // 🔥 Evitar problemas al reciclar el ViewHolder
        binding.cbApp.isChecked = isChecked // 🔥 Restaurar estado del CheckBox

        binding.cbApp.setOnCheckedChangeListener { _, checked ->
            onCheckedChange(checked) // 🔥 Notificar cambios al adaptador
        }

    }

}
