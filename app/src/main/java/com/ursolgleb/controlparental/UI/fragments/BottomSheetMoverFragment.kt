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
import com.ursolgleb.controlparental.databinding.BottomSheetMoverLayoutBinding
import com.ursolgleb.controlparental.utils.NavBarUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BottomSheetMoverFragment(
    private val app: AppEntity,
    private val icon: Drawable?
) : BottomSheetDialogFragment() {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    private var _binding: BottomSheetMoverLayoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetMoverLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        NavBarUtils.aplicarEstiloNavBar(this.dialog as Dialog)

        initUI()

        initListeners()


    }

    private fun initUI() {

        binding.tvAppName.text = app.appName
        Glide.with(requireContext())
            .load(icon)
            .into(binding.ivIconoApp)


        binding.moverSiempreBloqueadasLinearMarcado.visibility =
            if (app.blocked) View.VISIBLE else View.INVISIBLE
        binding.moverSiempreBloqueadasLinear.visibility =
            if (app.blocked) View.INVISIBLE else View.VISIBLE

        binding.moverEntretenimientoLinearMarcado.visibility =
            if (app.entretenimiento) View.VISIBLE else View.INVISIBLE
        binding.moverEntretenimientoLinear.visibility =
            if (app.entretenimiento) View.INVISIBLE else View.VISIBLE

        val esSiempreDisponible = !app.blocked && !app.entretenimiento
        binding.moverSiempreDisponiblesLinearMarcado.visibility =
            if (esSiempreDisponible) View.VISIBLE else View.INVISIBLE
        binding.moverSiempreDisponiblesLinear.visibility =
            if (esSiempreDisponible) View.INVISIBLE else View.VISIBLE


    }

    private fun initListeners() {
        binding.moverSiempreDisponiblesBoton.setOnClickListener {
            lifecycleScope.launch {
                appDataRepository.addAppsASiempreDisponiblesBD(listOf(app))
            }
            dismiss()
        }

        binding.moverEntretenimientoBoton.setOnClickListener {
            lifecycleScope.launch {
                appDataRepository.addAppsAEntretenimientoBD(listOf(app))
            }
            dismiss()
        }

        binding.moverSiempreBloqueadas.setOnClickListener {
            lifecycleScope.launch {
                appDataRepository.addAppsASiempreBloqueadasBD(listOf(app))
            }
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
