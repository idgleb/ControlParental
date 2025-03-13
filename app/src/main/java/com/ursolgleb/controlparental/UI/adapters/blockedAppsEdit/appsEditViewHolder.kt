package com.ursolgleb.controlparental.UI.adapters.marcarAppsParaBlockear

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ursolgleb.controlparental.UI.fragments.BottomSheetMoverFragment
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.databinding.ItemAppEditBinding

class appsEditViewHolder(
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
            val bottomSheetSheetMover = BottomSheetMoverFragment(app, icon)
            bottomSheetSheetMover.show(fragmentManager, "BottomSheetDialog")

        }
    }
}

