package com.ursolgleb.controlparental.UI.fragments

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.utils.NavBarUtils

class BottomSheetFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        NavBarUtils.aplicarEstiloNavBar(this.dialog as Dialog)

        super.onViewCreated(view, savedInstanceState)

        val opcion1 = view.findViewById<Button>(R.id.opcion1)
        val opcion2 = view.findViewById<Button>(R.id.opcion2)
        val cerrar = view.findViewById<Button>(R.id.cerrar)

        opcion1.setOnClickListener {
            Toast.makeText(requireContext(), "Opción 1 seleccionada", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        opcion2.setOnClickListener {
            Toast.makeText(requireContext(), "Opción 2 seleccionada", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        cerrar.setOnClickListener { dismiss() }
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
