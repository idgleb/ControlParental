package com.ursolgleb.controlparental.UI.adapters.marcarAppsPara

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.data.apps.entities.HorarioEntity
import com.ursolgleb.controlparental.databinding.ItemHorarioEditBinding

class HorarioEditAdapter(
    val horarios: MutableList<HorarioEntity>,
    val appDataRepository: AppDataRepository,
    private val fragmentManager: androidx.fragment.app.FragmentManager
) : RecyclerView.Adapter<HorarioEditViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorarioEditViewHolder {
        val binding =
            ItemHorarioEditBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HorarioEditViewHolder(binding, fragmentManager)
    }

    override fun onBindViewHolder(holder: HorarioEditViewHolder, position: Int) {
        val horario = horarios[position]

        holder.bind(horario)

    }


    override fun getItemCount(): Int = horarios.size

    //  Función para agregar una nueva app a la lista y actualizar la UI
    fun addAppEadaptador(horarios: HorarioEntity) {
        /*horarios.add(newApp)  // Agregar a la lista
        notifyItemInserted(horarios.size - 1)  // Notificar el cambio a RecyclerView*/
    }

    //  Función para actualizar toda la lista
    fun updateListEnAdaptador(newList: List<HorarioEntity>) {
       Log.w("AppsAdapter", "updateListAppEn blockedAppsEditAdapter")
        horarios.clear()
        horarios.addAll(newList)
        notifyDataSetChanged()
    }

}
