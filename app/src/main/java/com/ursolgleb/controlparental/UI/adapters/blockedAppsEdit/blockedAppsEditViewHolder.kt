package com.ursolgleb.controlparental.UI.adapters.marcarAppsParaBlockear

import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ursolgleb.controlparental.databinding.ItemAppEditBinding
import com.ursolgleb.controlparental.databinding.ItemAppGrandeBinding

class blockedAppsEditViewHolder(private val binding: ItemAppEditBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(appName: String?, icon: Drawable?, formattedTimeDeUso: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
        binding.tvAppName.text = appName ?: "No encontrada"
        Glide.with(binding.ivIconoApp.context)
            .load(icon) // Glide acepta `Drawable` y `Bitmap`
            .into(binding.ivIconoApp)
        binding.tvHorasDeUso.text = "Uso hoy: $formattedTimeDeUso"

        binding.itemAppEdit.setOnClickListener {
            //
        }


    }

}
