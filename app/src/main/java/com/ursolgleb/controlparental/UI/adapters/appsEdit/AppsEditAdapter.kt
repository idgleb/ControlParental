package com.ursolgleb.controlparental.UI.adapters.marcarAppsPara

import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.AppDataRepository
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.databinding.ItemAppEditBinding
import com.ursolgleb.controlparental.utils.AppsFun
import com.ursolgleb.controlparental.utils.Fun

class AppsEditAdapter(
    val apps: MutableList<AppEntity>,
    val appDataRepository: AppDataRepository,
    private val fragmentManager: androidx.fragment.app.FragmentManager
) : RecyclerView.Adapter<appsEditViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): appsEditViewHolder {
        val binding =
            ItemAppEditBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return appsEditViewHolder(binding, fragmentManager)
    }

    override fun onBindViewHolder(holder: appsEditViewHolder, position: Int) {
        val app = apps[position]

        val icon = BitmapDrawable(appDataRepository.context.resources, app.appIcon)

        val formattedTimeDeUso = Fun.formatearMiliSec(app.usageTimeToday)

        holder.bind(
            app,
            icon,
            formattedTimeDeUso
        )
    }

    override fun getItemCount(): Int = apps.size

    //  Función para agregar una nueva app a la lista y actualizar la UI
    fun addAppEadaptador(newApp: AppEntity) {
        apps.add(newApp)  // Agregar a la lista
        notifyItemInserted(apps.size - 1)  // Notificar el cambio a RecyclerView
    }

    //  Función para actualizar toda la lista
    fun updateListEnAdaptador(newList: List<AppEntity>) {
        Log.w("AppsAdapter", "updateListAppEn blockedAppsEditAdapter")
        apps.clear()
        apps.addAll(newList)
        notifyDataSetChanged()
    }

}
