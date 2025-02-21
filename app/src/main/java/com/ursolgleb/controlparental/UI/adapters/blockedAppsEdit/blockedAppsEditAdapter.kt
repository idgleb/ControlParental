package com.ursolgleb.controlparental.UI.adapters.marcarAppsParaBlockear

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.UI.viewmodel.SharedViewModel
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.databinding.ItemAppEditBinding
import com.ursolgleb.controlparental.utils.Fun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class blockedAppsEditAdapter(
    val apps: MutableList<AppEntity>,
    private val context: Context,
    private val sharedViewModel: SharedViewModel
) : RecyclerView.Adapter<blockedAppsEditViewHolder>() {

    private val selectedApps = mutableSetOf<String>() // 🔥 Almacena los paquetes de apps seleccionadas

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): blockedAppsEditViewHolder {
        val binding =
            ItemAppEditBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return blockedAppsEditViewHolder(binding)
    }

    override fun onBindViewHolder(holder: blockedAppsEditViewHolder, position: Int) {
        val app = apps[position]

        CoroutineScope(Dispatchers.IO).launch {
            val icon = sharedViewModel.getAppIcon(app.packageName, context)

            val formattedTimeDeUso = Fun.formatearTiempoDeUso(app.tiempoUsoSegundosHoy)

            withContext(Dispatchers.Main) {
                holder.bind(app.appName, icon, formattedTimeDeUso, selectedApps.contains(app.packageName)) { isChecked ->
                    if (isChecked) {
                        selectedApps.add(app.packageName) // ✅ Agrega a las apps seleccionadas
                    } else {
                        selectedApps.remove(app.packageName) // ✅ Elimina si se desmarca
                    }
                }
            }
        }
    }



    override fun getItemCount(): Int = apps.size

    fun getSelectedApps(): Set<String> = selectedApps // 🔥 Método para obtener apps seleccionadas

    // 🔥 ✅ Función para agregar una nueva app a la lista y actualizar la UI
    fun addAppEadaptador(newApp: AppEntity) {
        apps.add(newApp)  // Agregar a la lista
        notifyItemInserted(apps.size - 1)  // Notificar el cambio a RecyclerView

    }

    // 🔥 ✅ Función para actualizar toda la lista
    fun updateListEnAdaptador(newList: List<AppEntity>) {
        Log.w("AppsAdapter", "updateListAppEnAdaptador")
        apps.clear()
        apps.addAll(newList)
        notifyDataSetChanged()
    }

}
