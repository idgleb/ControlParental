package com.ursolgleb.controlparental.UI.adapters.marcarAppsPara

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.databinding.ItemAppGrandeBinding
import com.ursolgleb.controlparental.utils.Fun
import com.ursolgleb.controlparental.utils.StatusApp

class MarcarAppsParaAgregarViewHolder(private val binding: ItemAppGrandeBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(
        appName: String?,
        icon: Drawable?,
        formattedTimeDeUso: String,
        appStatus: String,
        context: Context,
        isChecked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        binding.tvAppName.text = appName ?: "No encontrada"
        Glide.with(binding.ivIconoApp.context)
            .load(icon) // Glide acepta `Drawable` y `Bitmap`
            .into(binding.ivIconoApp)
        binding.tvHorasDeUso.text = "Uso hoy: $formattedTimeDeUso"

        binding.cbApp.setOnCheckedChangeListener(null) // 🔥 Evitar problemas al reciclar el ViewHolder
        binding.cbApp.isChecked = isChecked // 🔥 Restaurar estado del CheckBox

        binding.cbApp.setOnCheckedChangeListener { _, checked ->
            onCheckedChange(checked) // 🔥 Notificar cambios al adaptador
        }


        when (appStatus) {
            StatusApp.BLOQUEADA.desc -> {
                binding.viewStatusApp.background =
                    ResourcesCompat.getDrawable(context.resources, R.drawable.lock, null)
                val params = binding.viewStatusApp.layoutParams
                params.width = Fun.dpToPx(20, binding.viewStatusApp)
                params.height = Fun.dpToPx(20, binding.viewStatusApp)
                binding.viewStatusApp.layoutParams = params
            }

            StatusApp.DISPONIBLE.desc -> {
                binding.viewStatusApp.background =
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.vecteezy_infinity,
                        null
                    )
                val params = binding.viewStatusApp.layoutParams
                params.width = Fun.dpToPx(20, binding.viewStatusApp)
                params.height = Fun.dpToPx(9, binding.viewStatusApp)
                binding.viewStatusApp.layoutParams = params
            }

            StatusApp.HORARIO.desc -> {
                binding.viewStatusApp.background =
                    ResourcesCompat.getDrawable(
                        context.resources,
                        R.drawable.vect_clock_timer,
                        null
                    )
                val params = binding.viewStatusApp.layoutParams
                params.width = Fun.dpToPx(33, binding.viewStatusApp)
                params.height = Fun.dpToPx(70, binding.viewStatusApp)
                binding.viewStatusApp.layoutParams = params
            }

            else -> {
                binding.viewStatusApp.background =
                    ResourcesCompat.getDrawable(context.resources, R.drawable.ic_check_white, null)
            }
        }


    }

}
