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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher

@AndroidEntryPoint
class PermisosFragment : Fragment(R.layout.fragment_permisos) {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    private var _binding: FragmentPermisosBinding? = null
    private val binding get() = _binding!!

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Launcher para permisos de ubicación
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    // Launcher para permiso de ubicación en segundo plano
    private lateinit var backgroundLocationPermissionLauncher: ActivityResultLauncher<String>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Registrar el launcher usando el helper de Permisos
        locationPermissionLauncher = Permisos.getLocationPermissionLauncher(this) { allGranted ->
            if (allGranted) {
                Log.d("PermisosFragment", "Permisos de ubicación otorgados")
                onResume()
            } else {
                Log.w("PermisosFragment", "Permisos de ubicación denegados")
            }
        }
        backgroundLocationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                Log.d("PermisosFragment", "Permiso de ubicación en segundo plano otorgado")
                onResume()
            } else {
                Log.w("PermisosFragment", "Permiso de ubicación en segundo plano denegado")
            }
        }

        initUI(view)

        initListeners()

    }

    private fun initUI(view: View) {
        _binding = FragmentPermisosBinding.bind(view)
    }

    override fun onResume() {
        super.onResume()
        
        // Actualizar UI para permisos de estadísticas de uso
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
        
        // Actualizar UI para permisos de ubicación
        if (Permisos.hasLocationPermission(appDataRepository.context)) {
            binding.requestLocationPermissionBoton.isEnabled = false
            binding.requestLocationPermissionBoton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ok_scaled,
                0,
                0,
                0
            )
            val color = ContextCompat.getColorStateList(appDataRepository.context, R.color.mercadopago)
            TextViewCompat.setCompoundDrawableTintList(binding.requestLocationPermissionBoton, color)
            // Mostrar botón de background location solo si falta ese permiso
            if (!Permisos.hasBackgroundLocationPermission(appDataRepository.context)) {
                binding.requestBackgroundLocationPermissionBoton.visibility = View.VISIBLE
                binding.requestBackgroundLocationPermissionBoton.isEnabled = true
                binding.requestBackgroundLocationPermissionBoton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.baseline_add_circle_outline_24,
                    0,
                    0,
                    0
                )
                val colorBg = ContextCompat.getColorStateList(appDataRepository.context, R.color.white)
                TextViewCompat.setCompoundDrawableTintList(binding.requestBackgroundLocationPermissionBoton, colorBg)
            } else {
                binding.requestBackgroundLocationPermissionBoton.visibility = View.VISIBLE
                binding.requestBackgroundLocationPermissionBoton.isEnabled = false
                binding.requestBackgroundLocationPermissionBoton.setCompoundDrawablesWithIntrinsicBounds(
                    R.drawable.ok_scaled,
                    0,
                    0,
                    0
                )
                val colorBg = ContextCompat.getColorStateList(appDataRepository.context, R.color.mercadopago)
                TextViewCompat.setCompoundDrawableTintList(binding.requestBackgroundLocationPermissionBoton, colorBg)
            }
        } else {
            binding.requestLocationPermissionBoton.isEnabled = true
            binding.requestLocationPermissionBoton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.baseline_add_circle_outline_24,
                0,
                0,
                0
            )
            val color = ContextCompat.getColorStateList(appDataRepository.context, R.color.white)
            TextViewCompat.setCompoundDrawableTintList(binding.requestLocationPermissionBoton, color)
            // Ocultar botón de background location si no tiene los normales
            binding.requestBackgroundLocationPermissionBoton.visibility = View.GONE
        }

        // Actualizar UI para permiso de ventana flotante (SYSTEM_ALERT_WINDOW)
        if (Permisos.hasSystemAlertWindowPermission(appDataRepository.context)) {
            binding.requestSystemAlertWindowPermissionBoton.isEnabled = false
            binding.requestSystemAlertWindowPermissionBoton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.ok_scaled,
                0,
                0,
                0
            )
            val color = ContextCompat.getColorStateList(appDataRepository.context, R.color.mercadopago)
            TextViewCompat.setCompoundDrawableTintList(binding.requestSystemAlertWindowPermissionBoton, color)
        } else {
            binding.requestSystemAlertWindowPermissionBoton.isEnabled = true
            binding.requestSystemAlertWindowPermissionBoton.setCompoundDrawablesWithIntrinsicBounds(
                R.drawable.baseline_add_circle_outline_24,
                0,
                0,
                0
            )
            val color = ContextCompat.getColorStateList(appDataRepository.context, R.color.white)
            TextViewCompat.setCompoundDrawableTintList(binding.requestSystemAlertWindowPermissionBoton, color)
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
        
        binding.requestLocationPermissionBoton.setOnClickListener {
            if (!Permisos.hasLocationPermission(appDataRepository.context)) {
                Log.w("PermisosFragment", "Solicitando permisos de ubicación")
                locationPermissionLauncher.launch(
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            } else {
                val msg = "Ya tienes los permisos de ubicación"
                Toast.makeText(appDataRepository.context, msg, Toast.LENGTH_SHORT).show()
                coroutineScope.launch { Archivo.appendTextToFile(requireContext(), "\n $msg") }
            }
        }

        binding.requestBackgroundLocationPermissionBoton.setOnClickListener {
            if (!Permisos.hasBackgroundLocationPermission(appDataRepository.context)) {
                Log.w("PermisosFragment", "Solicitando permiso de ubicación en segundo plano")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    backgroundLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            } else {
                Toast.makeText(appDataRepository.context, "Ya tienes el permiso de ubicación en segundo plano", Toast.LENGTH_SHORT).show()
            }
        }

        binding.requestSystemAlertWindowPermissionBoton.setOnClickListener {
            if (!Permisos.hasSystemAlertWindowPermission(appDataRepository.context)) {
                Log.w("PermisosFragment", "Solicitando permiso de ventana flotante (SYSTEM_ALERT_WINDOW)")
                Permisos.requestSystemAlertWindowPermission(appDataRepository.context)
            } else {
                val msg = "Ya tienes el permiso de ventana flotante"
                Toast.makeText(appDataRepository.context, msg, Toast.LENGTH_SHORT).show()
                coroutineScope.launch { Archivo.appendTextToFile(requireContext(), "\n $msg") }
            }
        }

        binding.okBoton.setOnClickListener {
            if (Permisos.hasUsageStatsPermission(appDataRepository.context) && Permisos.hasLocationPermission(appDataRepository.context)) {
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
