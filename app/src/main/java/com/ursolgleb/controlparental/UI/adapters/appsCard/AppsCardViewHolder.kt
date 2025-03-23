package com.ursolgleb.controlparental.UI.adapters.appsCard

import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ursolgleb.controlparental.databinding.ItemBlockedAppBinding

class AppsCardViewHolder(private val binding: ItemBlockedAppBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(appName: String?, icon: Drawable?) {
        binding.textViewPackageName.text = appName ?: "No encontrada"
        Glide.with(binding.ivIconoApp.context)
            .load(icon) // Glide acepta `Drawable` y `Bitmap`
            .into(binding.ivIconoApp)
    }

}
