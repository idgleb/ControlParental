package com.ursolgleb.controlparental.UI.adapters.appsCard

import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.AppDataRepository
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.databinding.ItemBlockedAppBinding
import com.ursolgleb.controlparental.utils.AppsFun
import javax.inject.Inject

class AppsCardAdapter @Inject constructor(
    val apps: MutableList<AppEntity>,
    val appDataRepository: AppDataRepository
) : RecyclerView.Adapter<AppsCardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsCardViewHolder {
        val binding =
            ItemBlockedAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppsCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppsCardViewHolder, position: Int) {
        val app = apps[position]
        val icon = BitmapDrawable(appDataRepository.context.resources, app.appIcon)
        holder.bind(app.appName, icon)
    }

    override fun getItemCount(): Int = apps.size


    // 🔥 ✅ Función para actualizar toda la lista
    fun updateListEnAdaptador(newList: List<AppEntity>) {
        apps.clear()
        apps.addAll(newList)
        notifyDataSetChanged()
    }

}
