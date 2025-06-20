package com.ursolgleb.controlparental.UI.adapters.marcarAppsPara

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.data.apps.entities.HorarioEntity
import com.ursolgleb.controlparental.databinding.ItemHorarioEditBinding
import com.ursolgleb.controlparental.handlers.SyncHandler

class HorarioEditAdapter(
    val horarios: MutableList<HorarioEntity>,
    val appDataRepository: AppDataRepository,
    val syncHendler: SyncHandler,
    private val fragmentManager: androidx.fragment.app.FragmentManager
) : RecyclerView.Adapter<HorarioEditViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorarioEditViewHolder {
        val binding =
            ItemHorarioEditBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HorarioEditViewHolder(binding, fragmentManager, appDataRepository, syncHendler)
    }

    override fun onBindViewHolder(holder: HorarioEditViewHolder, position: Int) {
        val horario = horarios[position]

        holder.bind(horario)

    }


    override fun getItemCount(): Int = horarios.size

    //  Función para actualizar toda la lista
    fun updateListEnAdaptador(newList: List<HorarioEntity>) {
       Log.w("AppsAdapter", "updateListAppEn blockedAppsEditAdapter")
        horarios.clear()
        horarios.addAll(newList)
        notifyDataSetChanged()
    }

}
