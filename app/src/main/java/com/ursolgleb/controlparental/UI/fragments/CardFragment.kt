package com.ursolgleb.controlparental.UI.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.UI.adapters.appsCard.AppsCardAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.AppDataRepository
import com.ursolgleb.controlparental.data.local.dao.AppDao
import com.ursolgleb.controlparental.databinding.FragmentCardBinding
import com.ursolgleb.controlparental.utils.Fun
import com.ursolgleb.controlparental.utils.StatusApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CardFragment(val categoria: StatusApp) : Fragment(R.layout.fragment_card) {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    lateinit var appDao: AppDao

    private var _binding: FragmentCardBinding? = null
    private val binding get() = _binding!!

    private var appCardAdapter: AppsCardAdapter? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appDao = appDataRepository.appDatabase.appDao()

        initUI(view)
        initListeners()
        initObservers()
    }

    private fun initUI(view: View) {
        _binding = FragmentCardBinding.bind(view)

        appCardAdapter = AppsCardAdapter(mutableListOf(), appDataRepository)
        binding.rvApps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvApps.adapter = appCardAdapter
        binding.rvApps.setRecycledViewPool(RecyclerView.RecycledViewPool())

        when (categoria.desc) {
            StatusApp.BLOQUEADA.desc -> {
                binding.iconDeLista.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.lock, null)
                val params = binding.iconDeLista.layoutParams
                params.width = Fun.dpToPx(20, binding.iconDeLista)
                params.height = Fun.dpToPx(20, binding.iconDeLista)
                binding.iconDeLista.layoutParams = params

            }

            StatusApp.DISPONIBLE.desc -> {
                binding.iconDeLista.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.vecteezy_infinity, null)
                val params = binding.iconDeLista.layoutParams
                params.width = Fun.dpToPx(20, binding.iconDeLista)
                params.height = Fun.dpToPx(12, binding.iconDeLista)
                binding.iconDeLista.layoutParams = params
            }

            StatusApp.HORARIO.desc -> {
                binding.iconDeLista.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.vect_clock_timer, null)
                val params = binding.iconDeLista.layoutParams
                params.width = Fun.dpToPx(34, binding.iconDeLista)
                params.height = Fun.dpToPx(75, binding.iconDeLista)
                binding.iconDeLista.layoutParams = params
                binding.clCambiarHorarioLimite.visibility = View.VISIBLE
            }

            else -> {
                binding.iconDeLista.background =
                    ResourcesCompat.getDrawable(resources, R.drawable.ic_check_white, null)
            }
        }

        binding.tvNombreLista.text = when (categoria.desc) {
            StatusApp.BLOQUEADA.desc -> "Bloqueadas siempre"
            StatusApp.DISPONIBLE.desc -> "Disponibles siempre"
            StatusApp.HORARIO.desc -> "Bajo de horario y limite"
            else -> "Apps ..."
        }

        binding.aggregarAppsABoton.text = when (categoria.desc) {
            StatusApp.BLOQUEADA.desc -> "Agregar a siempre bloqueadas"
            StatusApp.DISPONIBLE.desc -> "Agregar a siempre disponibles"
            StatusApp.HORARIO.desc -> "Agregar a bajo de horario y limite"
            else -> "Agregar a ..."
        }

    }

    private fun initListeners() {

        binding.aggregarAppsABoton.setOnClickListener {
            val action =
                MainAdminFragmentDirections.actionGlobalAddAppsA(category = categoria.desc)
            findNavController().navigate(action)
        }

        binding.cvAppsDispon.setOnClickListener {
            navegarADisponAppsEdit()
        }
        binding.tvEmptyMessage.setOnClickListener {
            navegarADisponAppsEdit()
        }


    }

    private fun navegarADisponAppsEdit() {
        val action =
            MainAdminFragmentDirections.actionGlobalAppsEditFragment(category = categoria.desc)
        findNavController().navigate(action)
    }

    private fun initObservers() {

        val listaDeObservarFlow = when (categoria.desc) {
            StatusApp.BLOQUEADA.desc -> appDataRepository.blockedAppsFlow
            StatusApp.DISPONIBLE.desc -> appDataRepository.disponAppsFlow
            StatusApp.HORARIO.desc -> appDataRepository.horarioAppsFlow
            else -> appDataRepository.todosAppsFlow
        }

        // 🔥 Observar cambios en la lista de apps dispon
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                listaDeObservarFlow.collect { newList ->
                    Log.w("CardFragment", "Lista de apps ${categoria.desc} actualizada: $newList")
                    binding.tvCantidadApps.text = newList.size.toString()
                    appCardAdapter?.updateListEnAdaptador(newList.take(3))
                    // 🔥 Si la lista está vacía, mostrar "Empty"
                    binding.tvEmptyMessage.text = if (newList.isEmpty()) {
                        "No hay aplicaciones ${categoria.desc}"
                    } else ""
                }
            }
        }

        // 🔥 Observar si updateBDApps() esta en processo
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                appDataRepository.mutexUpdateBDAppsStateFlow.collect { isLocked ->
                    // Aquí se actualiza cada vez que cambia el estado del mutex.
                    if (isLocked) {
                        // Mostrar un indicador de carga o bloquear la UI.
                        Log.d("UI", "La operación updateBDApps está en curso")
                        binding.progressBarUpdateBD.visibility = View.VISIBLE
                    } else {
                        // Ocultar el indicador de carga o desbloquear la UI.
                        Log.d("UI", "La operación updateBDApps ha finalizado")
                        binding.progressBarUpdateBD.visibility = View.GONE
                    }
                }
            }
        }
    }



    override fun onDestroyView() {
        super.onDestroyView()
        Log.e("BlockedAppsFragment", "onDestroyView")
        _binding = null // 🔥 Evitar memory leaks
    }

}
