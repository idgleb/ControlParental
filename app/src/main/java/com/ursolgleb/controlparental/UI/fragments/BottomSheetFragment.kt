package com.ursolgleb.controlparental.UI.fragments

import android.app.Dialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ursolgleb.controlparental.AppDataRepository
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.utils.NavBarUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BottomSheetFragment(
    private val app: AppEntity,
    private val icon: Drawable?
) : BottomSheetDialogFragment() {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        NavBarUtils.aplicarEstiloNavBar(this.dialog as Dialog)

        // 🔥 Establecer nombre e icono de la app
        val ivIconoApp = view.findViewById<ImageView>(R.id.ivIconoApp)
        val tvAppName = view.findViewById<TextView>(R.id.tvAppName)

        tvAppName.text = app.appName
        Glide.with(requireContext())
            .load(icon)
            .into(ivIconoApp)


        val moverSiempreDisponiblesBoton =
            view.findViewById<Button>(R.id.moverSiempreDisponiblesBoton)
        val moverEntretenimientoBoton = view.findViewById<Button>(R.id.moverEntretenimientoBoton)
        val moverSiempreBloqueadas = view.findViewById<Button>(R.id.moverSiempreBloqueadas)

        moverSiempreDisponiblesBoton.setOnClickListener {
            lifecycleScope.launch {
                appDataRepository.addAppsASiempreDisponiblesBD(listOf(app))
            }
            Toast.makeText(requireContext(), "Lista actualizada", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        moverEntretenimientoBoton.setOnClickListener {
            lifecycleScope.launch {
                appDataRepository.addAppsAEntretenimientoBD(listOf(app))
            }
            Toast.makeText(requireContext(), "Lista actualizada", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        moverSiempreBloqueadas.setOnClickListener {
            lifecycleScope.launch {
                appDataRepository.addAppsASiempreBloqueadasBD(listOf(app))
            }
            Toast.makeText(requireContext(), "Lista actualizada", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED // Se abre completamente
                behavior.skipCollapsed = true // Permite cerrar deslizando hacia abajo
            }
        }
        return dialog
    }


}
