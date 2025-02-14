package com.ursolgleb.controlparental.UI.fragments

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.UI.adapters.blockedApps.BlockedAppsAdapter
import com.ursolgleb.controlparental.UI.viewmodel.SharedViewModel
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.ControlParentalApp
import com.ursolgleb.controlparental.UI.activities.AddAppsActivity
import com.ursolgleb.controlparental.data.local.AppDatabase
import com.ursolgleb.controlparental.data.local.dao.AppDao
import com.ursolgleb.controlparental.data.local.dao.BlockedDao
import com.ursolgleb.controlparental.databinding.FragmentBlockedAppsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class BlockedAppsFragment : Fragment(R.layout.fragment_blocked_apps) {

    @Inject
    lateinit var appDatabase: AppDatabase
    lateinit var appDao: AppDao
    lateinit var blockedDao: BlockedDao

    private var _binding: FragmentBlockedAppsBinding? = null
    private val binding get() = _binding!!

    private var blockedAppAdapter: BlockedAppsAdapter? = null
    private val sharedViewModel: SharedViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appDao = appDatabase.appDao()
        blockedDao = appDatabase.blockedDao()

        initUI(view)

        initListeners()

        initObservers()
    }

    private fun initListeners() {

        binding.delitBlackedAppBoton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                blockedDao.deleteAllBlockedApps()
                withContext(Dispatchers.Main) {
                    Log.w("BlockedAppsFragment", "Apps delited")
                }
            }
        }

        binding.aggregarAppsABlockedBoton.setOnClickListener {
            //val intent = Intent(requireContext(), AddAppsActivity::class.java)
            //startActivity(intent)
            //findNavController().navigate(R.id.action_blockedAppsFragment_to_addAppsFragment)

        }

        binding.delitAllAppBoton.setOnClickListener {
            lifecycleScope.launch {
                appDao.deleteAllApps()
                withContext(Dispatchers.Main) {
                    Log.e("BlockedAppsFragment", "Todos Apps delited")
                }
            }
        }

        binding.llenarAllAppBoton.setOnClickListener {
            lifecycleScope.launch { sharedViewModel.updateBDApps() }
        }

    }

    private fun initObservers() {
        // 🔥 Observar cambios en la lista de apps bloqueadas

/*        lifecycleScope.launch {
            sharedViewModel.blockedApps.collect { newList ->
                Log.w("BlockedAppsFragment", "Lista de apps bloqueadas actualizada: $newList")
                blockedAppAdapter?.updateListEnAdaptador(newList)
                // 🔥 Si la lista está vacía, mostrar "Empty"
                if (newList.isEmpty()) {
                    binding.tvEmptyMessage.visibility = View.VISIBLE
                    binding.rvAppsBloqueadas.visibility = View.GONE
                } else {
                    binding.tvEmptyMessage.visibility = View.GONE
                    binding.rvAppsBloqueadas.visibility = View.VISIBLE
                }
            }
        }*/


        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                sharedViewModel.blockedApps.collect { newList ->
                    // Aquí binding ya está seguro, porque la colección se detiene cuando la vista se destruye
                    Log.w("BlockedAppsFragment", "Lista de apps bloqueadas actualizada: $newList")
                    blockedAppAdapter?.updateListEnAdaptador(newList)
                    // 🔥 Si la lista está vacía, mostrar "Empty"
                    if (newList.isEmpty()) {
                        binding.tvEmptyMessage.visibility = View.VISIBLE
                        binding.rvAppsBloqueadas.visibility = View.GONE
                    } else {
                        binding.tvEmptyMessage.visibility = View.GONE
                        binding.rvAppsBloqueadas.visibility = View.VISIBLE
                    }
                }


            }
        }



    }

    private fun initUI(view: View) {
        _binding = FragmentBlockedAppsBinding.bind(view)

        blockedAppAdapter = BlockedAppsAdapter(mutableListOf(), requireContext(), appDatabase)
        binding.rvAppsBloqueadas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAppsBloqueadas.adapter = blockedAppAdapter
        binding.rvAppsBloqueadas.setRecycledViewPool(RecyclerView.RecycledViewPool()) // ✅ Optimización
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 🔥 Evitar memory leaks
    }
}
