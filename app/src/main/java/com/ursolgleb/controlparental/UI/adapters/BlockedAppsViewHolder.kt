package com.ursolgleb.controlparental.UI.adapters

import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ursolgleb.controlparental.data.local.entities.BlockedEntity
import com.ursolgleb.controlparental.databinding.ItemBlockedAppBinding

class BlockedAppsViewHolder(private val binding: ItemBlockedAppBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(blockedApp: BlockedEntity, appName: String?, icon: Drawable?) {
        binding.textViewPackageName.text = appName ?: "No encontrada"
        Glide.with(binding.ivIconoApp.context)
            .load(icon) // Glide acepta `Drawable` y `Bitmap`
            .into(binding.ivIconoApp)
    }

}
