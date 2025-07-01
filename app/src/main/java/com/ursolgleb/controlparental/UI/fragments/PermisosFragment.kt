package com.ursolgleb.controlparental.UI.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.ursolgleb.controlparental.data.local.AppDataRepository
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.databinding.FragmentPermisosBinding
import com.ursolgleb.controlparental.utils.Archivo
import com.ursolgleb.controlparental.utils.Permisos
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PermisosFragment : Fragment(R.layout.fragment_permisos) {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    private var _binding: FragmentPermisosBinding? = null
    private val binding get() = _binding!!

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI(view)

        initListeners()

    }

    private fun initUI(view: View) {
        _binding = FragmentPermisosBinding.bind(view)
    }

    override fun onResume() {
        super.onResume()
        if (Permisos.hasUsageStatsPermission(appDataRepository.context)) {
            binding.requestUsageStatsPermissionBoton.isEnabled = false
            binding.requestUsageStatsPermissionBoton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ok_scaled,
                0,
                0,
                0
            )
            val color = ContextCompat.getColorStateList(appDataRepository.context, R.color.mercadopago)
            TextViewCompat.setCompoundDrawableTintList(binding.requestUsageStatsPermissionBoton, color)
        } else {
            binding.requestUsageStatsPermissionBoton.isEnabled = true
            binding.requestUsageStatsPermissionBoton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.baseline_add_circle_outline_24,
                0,
                0,
                0
            )
            val color = ContextCompat.getColorStateList(appDataRepository.context, R.color.white)
            TextViewCompat.setCompoundDrawableTintList(binding.requestUsageStatsPermissionBoton, color)
        }
    }

    private fun initListeners() {

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.requestUsageStatsPermissionBoton.setOnClickListener {
            if (!Permisos.hasUsageStatsPermission(appDataRepository.context)) {
                Log.w("PermisosFragment", "Solicitando permiso de Usage Stats")
                Permisos.requestUsageStatsPermission(appDataRepository.context)
            } else {
                val msg = "Ya tienes el permiso de Usage Stats"
                Toast.makeText(appDataRepository.context, msg, Toast.LENGTH_SHORT).show()
                coroutineScope.launch { Archivo.appendTextToFile(requireContext(), "\n $msg") }
            }
        }

        binding.okBoton.setOnClickListener {
            if (Permisos.hasUsageStatsPermission(appDataRepository.context)) {
                findNavController().popBackStack()
            } else {
                Toast.makeText(
                    requireContext(),
                    "No todos los permisos diste",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Evitar memory leaks
        coroutineScope.cancel()
    }
}
