package com.ursolgleb.controlparental.UI.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.ursolgleb.controlparental.data.local.AppDataRepository
import com.ursolgleb.controlparental.databinding.BottomSheetPermisosLayoutBinding
import com.ursolgleb.controlparental.utils.NavBarUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BottomSheetPermisosFragment : BottomSheetDialogFragment() {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    private var _binding: BottomSheetPermisosLayoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPermisosLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        NavBarUtils.aplicarEstiloNavBar(this.dialog as Dialog)

        initListeners()

    }


    private fun initListeners() {
        binding.darPermisosBoton.setOnClickListener {

            val action =
                PermisosFragmentDirections.actionGlobalPermisosFragment()
            findNavController().navigate(action)

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
