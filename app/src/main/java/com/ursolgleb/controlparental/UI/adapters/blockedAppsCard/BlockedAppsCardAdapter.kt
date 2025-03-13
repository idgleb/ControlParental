package com.ursolgleb.controlparental.UI.adapters.blockedAppsCard

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.AppDataRepository
import com.ursolgleb.controlparental.data.local.AppDatabase
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.BlockedEntity
import com.ursolgleb.controlparental.databinding.ItemBlockedAppBinding
import com.ursolgleb.controlparental.utils.AppsFun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BlockedAppsCardAdapter @Inject constructor(
    val blockedApps: MutableList<AppEntity>,
    val appDataRepository: AppDataRepository
) : RecyclerView.Adapter<BlockedAppsCardViewHolder>() {

    val appDao = appDataRepository.appDatabase.appDao()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockedAppsCardViewHolder {
        val binding =
            ItemBlockedAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BlockedAppsCardViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BlockedAppsCardViewHolder, position: Int) {
        val blockedApp = blockedApps[position]
        val icon = AppsFun.getAppIcon(appDataRepository.context,blockedApp.packageName)
        holder.bind(blockedApp.appName, icon)
    }

    override fun getItemCount(): Int = blockedApps.size

    // 🔥 ✅ Función para agregar una nueva app bloqueada a la lista y actualizar la UI
    fun addBlockedAppEadaptador(newBlockedApp: AppEntity) {
        blockedApps.add(newBlockedApp)  // Agregar a la lista
        notifyItemInserted(blockedApps.size - 1)  // Notificar el cambio a RecyclerView

    }

    // 🔥 ✅ Función para actualizar toda la lista
    fun updateListEnAdaptador(newList: List<AppEntity>) {
        blockedApps.clear()
        blockedApps.addAll(newList)
        notifyDataSetChanged()
    }

}
