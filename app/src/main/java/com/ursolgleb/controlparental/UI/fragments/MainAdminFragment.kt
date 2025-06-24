package com.ursolgleb.controlparental.UI.fragments

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.UI.activities.DesarolloActivity
import com.ursolgleb.controlparental.databinding.FragmentMainAdminBinding
import com.ursolgleb.controlparental.utils.Permisos
import com.ursolgleb.controlparental.utils.Session
import com.ursolgleb.controlparental.utils.StatusApp
import com.ursolgleb.controlparental.UI.viewmodel.SharedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainAdminFragment : Fragment(R.layout.fragment_main_admin) {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    @Inject
    lateinit var session: Session

    private var _binding: FragmentMainAdminBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: SharedViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentMainAdminBinding.bind(view)

        // Configuración edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Log.e("MainAdminFragment", "onViewCreated")

        initUI()
        initListeners()
        initObservers()
    }

    private fun initUI() {

        initHeightDeSvInfo()

        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_apps_horario, CardFragment.newInstance(StatusApp.HORARIO))
            .commit()

        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_apps_disponibles, CardFragment.newInstance(StatusApp.DISPONIBLE))
            .commit()

        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_apps_bloqueadas, CardFragment.newInstance(StatusApp.BLOQUEADA))
            .commit()

        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_desarollo, DesarolloCardFragment())
            .commit()


    }

    override fun onStart() {
        super.onStart()
        if (!Permisos.hasUsageStatsPermission(appDataRepository.context)) {

            val bottomSheetPermisos = BottomSheetPermisosFragment()
            bottomSheetPermisos.show(parentFragmentManager, "BottomSheetDialog")

        }
    }

    private fun initListeners() {
        binding.ayudaBoton.setOnClickListener {
            val intent = Intent(requireContext(), DesarolloActivity::class.java)
            startActivity(intent)
        }
        binding.btnSalir.setOnClickListener {
            session.cerrarSesion()
            requireActivity().finish()
        }
        // Copiar deviceId al portapapeles
        binding.btnCopyDeviceId.setOnClickListener {
            val deviceId = binding.tvDeviceId.text.toString()
            val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Device ID", deviceId)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "ID copiado al portapapeles", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initObservers() {
        // 🔥 Observar si se necesita mostrar el mostrarBottomSheetActualizada
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appDataRepository.mostrarBottomSheetActualizadaFlow.collect { isTrue ->
                    if (isTrue) {
                        // Mostrar un indicador de carga o bloquear la UI.
                        val bottomSheetActualizada = BottomSheetActualizadaFragment()
                        bottomSheetActualizada.show(parentFragmentManager, "BottomSheetDialog")
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appDataRepository.syncStatusFlow.collect { syncStatus ->
                    binding.tvHola.text = syncStatus
                }
            }
        }

        // Observar el deviceId y mostrarlo en el TextView
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sharedViewModel.deviceEntity.collect { device ->
                    binding.tvDeviceId.text = device?.deviceId ?: "Sin ID"
                }
            }
        }
    }

    private fun initHeightDeSvInfo() {
        binding.svInfo.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                binding.svInfo.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val scrollViewHeight = binding.svInfo.height
                // Calcula el 50% del alto y lo aplica a vFondo
                val newHeight = (scrollViewHeight * 0.55).toInt()
                val params = binding.vFondo.layoutParams
                params.height = newHeight
                binding.vFondo.layoutParams = params
            }
        })
    }

    override fun onResume() {
        super.onResume()
        //appDataRepository.updateBDApps(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Evitar memory leaks
    }
}
