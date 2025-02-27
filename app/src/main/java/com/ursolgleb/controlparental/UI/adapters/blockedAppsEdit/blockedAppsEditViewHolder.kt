package com.ursolgleb.controlparental.UI.adapters.marcarAppsParaBlockear

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ursolgleb.controlparental.UI.fragments.BottomSheetFragment
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.databinding.ItemAppEditBinding
import com.ursolgleb.controlparental.databinding.ItemAppGrandeBinding

class blockedAppsEditViewHolder(
    private val binding: ItemAppEditBinding,
    private val fragmentManager: androidx.fragment.app.FragmentManager
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        app: AppEntity,
        icon: Drawable?,
        formattedTimeDeUso: String,
        isChecked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        binding.tvAppName.text = app.appName
        Glide.with(binding.ivIconoApp.context)
            .load(icon)
            .into(binding.ivIconoApp)
        binding.tvHorasDeUso.text = "Uso hoy: $formattedTimeDeUso"

        binding.itemAppEdit.setOnClickListener {
            Log.w("BottomSheetFragment", "onClick")
            val bottomSheet = BottomSheetFragment(app, icon)
            bottomSheet.show(fragmentManager, "BottomSheetDialog")
        }
    }
}

