package com.ursolgleb.controlparental.UI.fragments

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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.ControlParentalApp
import com.ursolgleb.controlparental.UI.activities.AddAppsActivity
import com.ursolgleb.controlparental.databinding.FragmentBlockedAppsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockedAppsFragment : Fragment(R.layout.fragment_blocked_apps) {
    private var blockedAppAdapter: BlockedAppsAdapter? = null
    private val sharedViewModel: SharedViewModel by activityViewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }
    private var _binding: FragmentBlockedAppsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI(view)

        initListeners()

        initObservers()
    }

    private fun initListeners() {

        binding.delitBlackedAppBoton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val blockedDao = ControlParentalApp.dbApps.blockedDao()
                blockedDao.deleteAllBlockedApps()
                withContext(Dispatchers.Main) {
                    Log.w("BlockedAppsFragment", "Apps delited")
                }
            }
        }

        binding.aggregarAppsABlockedBoton.setOnClickListener {
            val intent = Intent(requireContext(), AddAppsActivity::class.java)
            startActivity(intent)
        }

        binding.delitAllAppBoton.setOnClickListener {
            lifecycleScope.launch {
                ControlParentalApp.dbApps.appDao().deleteAllApps()
                //ControlParentalApp.dbApps.blockedDao().deleteAllBlockedApps()
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
        lifecycleScope.launch {
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
        }
    }

    private fun initUI(view: View) {
        _binding = FragmentBlockedAppsBinding.bind(view)

        lifecycleScope.launch {
            val blockedApps = sharedViewModel.getBlockedAppsFromDB()
            Log.w("BlockedAppsFragment", "initUI getBlockedAppsFromDB(): $blockedApps")
            withContext(Dispatchers.Main) {
                Log.w("BlockedAppsFragment", "initUI crear ADAPTER: $blockedApps")
                blockedAppAdapter =
                    BlockedAppsAdapter(blockedApps.toMutableList(), requireContext())
                binding.rvAppsBloqueadas.layoutManager = LinearLayoutManager(requireContext())
                binding.rvAppsBloqueadas.adapter = blockedAppAdapter
                binding.rvAppsBloqueadas.setRecycledViewPool(RecyclerView.RecycledViewPool()) // ✅ Optimización
            }
        }


    }


    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { sharedViewModel.updateBDApps() }
        Log.w("BlockedAppsFragment", "onResume")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 🔥 Evitar memory leaks
    }
}
