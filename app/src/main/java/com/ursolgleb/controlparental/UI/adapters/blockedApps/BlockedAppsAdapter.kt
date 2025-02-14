package com.ursolgleb.controlparental.UI.adapters.blockedApps

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.ControlParentalApp
import com.ursolgleb.controlparental.data.local.AppDatabase
import com.ursolgleb.controlparental.data.local.entities.BlockedEntity
import com.ursolgleb.controlparental.databinding.ItemBlockedAppBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BlockedAppsAdapter@Inject constructor(
    val blockedApps: MutableList<BlockedEntity>,
    private val context: Context,
    appDatabase: AppDatabase
) : RecyclerView.Adapter<BlockedAppsViewHolder>() {

    val appDao = appDatabase.appDao()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BlockedAppsViewHolder {
        val binding =
            ItemBlockedAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BlockedAppsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BlockedAppsViewHolder, position: Int) {
        val blockedApp = blockedApps[position]

        CoroutineScope(Dispatchers.IO).launch {
            val appInfo = appDao.getApp(blockedApp.packageName)
            val icon = getAppIcon(blockedApp.packageName, context)

            withContext(Dispatchers.Main) {
                holder.bind(appInfo?.appName, icon)
            }
        }
    }

    override fun getItemCount(): Int = blockedApps.size

    private suspend fun getAppIcon(packageName: String, context: Context): Drawable? {
        return withContext(Dispatchers.IO) {
            try {
                val packageManager = context.packageManager
                packageManager.getApplicationIcon(packageName)
            } catch (e: Exception) {
                null
            }
        }
    }

    // 🔥 ✅ Función para agregar una nueva app bloqueada a la lista y actualizar la UI
    fun addBlockedAppEadaptador(newBlockedApp: BlockedEntity) {
        blockedApps.add(newBlockedApp)  // Agregar a la lista
        notifyItemInserted(blockedApps.size - 1)  // Notificar el cambio a RecyclerView

    }

    // 🔥 ✅ Función para actualizar toda la lista
    fun updateListEnAdaptador(newList: List<BlockedEntity>) {
        blockedApps.clear()
        blockedApps.addAll(newList)
        notifyDataSetChanged()
    }

}
