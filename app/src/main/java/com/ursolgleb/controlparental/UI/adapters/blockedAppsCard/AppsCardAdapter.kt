package com.ursolgleb.controlparental.UI.adapters.blockedAppsCard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.AppDataRepository
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.databinding.ItemBlockedAppBinding
import com.ursolgleb.controlparental.utils.AppsFun
import javax.inject.Inject

class AppsCardAdapter @Inject constructor(
    val blockedApps: MutableList<AppEntity>,
    val appDataRepository: AppDataRepository
) : RecyclerView.Adapter<AppsCardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsCardViewHolder {
        val binding =
            ItemBlockedAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppsCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppsCardViewHolder, position: Int) {
        val blockedApp = blockedApps[position]
        val icon = AppsFun.getAppIcon(appDataRepository.context,blockedApp.packageName)
        holder.bind(blockedApp.appName, icon)
    }

    override fun getItemCount(): Int = blockedApps.size


    // 🔥 ✅ Función para actualizar toda la lista
    fun updateListEnAdaptador(newList: List<AppEntity>) {
        blockedApps.clear()
        blockedApps.addAll(newList)
        notifyDataSetChanged()
    }

}
