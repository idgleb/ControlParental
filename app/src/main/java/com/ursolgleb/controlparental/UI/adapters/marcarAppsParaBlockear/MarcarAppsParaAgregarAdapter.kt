package com.ursolgleb.controlparental.UI.adapters.marcarAppsParaBlockear

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.AppDataRepository
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.databinding.ItemAppGrandeBinding
import com.ursolgleb.controlparental.utils.AppsFun
import com.ursolgleb.controlparental.utils.Fun

class MarcarAppsParaAgregarAdapter(
    val apps: MutableList<AppEntity>,
    val appDataRepository: AppDataRepository
) : RecyclerView.Adapter<MarcarAppsParaAgregarViewHolder>() {


    private val selectedApps =
        mutableSetOf<AppEntity>() // 🔥 Almacena los paquetes de apps seleccionadas

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MarcarAppsParaAgregarViewHolder {
        val binding =
            ItemAppGrandeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MarcarAppsParaAgregarViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MarcarAppsParaAgregarViewHolder, position: Int) {
        val app = apps[position]

        val icon = AppsFun.getAppIcon(appDataRepository.context, app.packageName)

        val formattedTimeDeUso = Fun.formatearMiliSec(app.tiempoUsoHoy)

        holder.bind(
            app.appName,
            icon,
            formattedTimeDeUso,
            selectedApps.contains(app)
        ) { isChecked ->
            if (isChecked) {
                selectedApps.add(app) // ✅ Agrega a las apps seleccionadas
            } else {
                selectedApps.remove(app) // ✅ Elimina si se desmarca
            }
        }

    }

    override fun getItemCount(): Int = apps.size

    fun getSelectedApps(): Set<AppEntity> =
        selectedApps // 🔥 Método para obtener apps seleccionadas

    // 🔥 ✅ Función para agregar una nueva app a la lista y actualizar la UI
    fun addAppEadaptador(newApp: AppEntity) {
        apps.add(newApp)  // Agregar a la lista
        notifyItemInserted(apps.size - 1)  // Notificar el cambio a RecyclerView

    }

    // 🔥 ✅ Función para actualizar toda la lista
    fun updateListEnAdaptador(newList: List<AppEntity>) {
        Log.w("AppsAdapter", "updateListAppEn MarcarAppsParaBloquearAdapter")
        apps.clear()
        apps.addAll(newList)
        notifyDataSetChanged()
    }

}
