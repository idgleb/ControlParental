package com.ursolgleb.controlparental.UI.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.UI.adapters.BlockedAppsAdapter
import com.ursolgleb.controlparental.UI.viewmodel.SharedViewModel
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.ursolgleb.controlparental.ControlParentalApp
import com.ursolgleb.controlparental.databinding.FragmentBlockedAppsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockedAppsFragment : Fragment(R.layout.fragment_blocked_apps) {
    lateinit var blockedAppAdapter: BlockedAppsAdapter
    private val sharedViewModel: SharedViewModel by activityViewModels()
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
                    sharedViewModel.updateBlockedAppsInterfaz(emptyList())
                    Log.w("BlockedAppsFragment", "Apps delited")
                }
            }
        }

        binding.aggregarAppsABlockedBoton.setOnClickListener {
            sharedViewModel.addAppBlockList("com.android.chrome",
                onSuccess = { msg -> Log.w("BlockedAppsFragment", msg) },
                onError = { msg -> Log.w("BlockedAppsFragment", msg) }
            )
            sharedViewModel.addAppBlockList("com.google.android.youtube",
                onSuccess = { msg -> Log.w("BlockedAppsFragment", msg) },
                onError = { msg -> Log.w("BlockedAppsFragment", msg) }
            )
        }

        binding.delitAllAppBoton.setOnClickListener {
            lifecycleScope.launch {
                ControlParentalApp.dbApps.appDao().deleteAllApps()
                //ControlParentalApp.dbApps.blockedDao().deleteAllBlockedApps()
                withContext(Dispatchers.Main) {
                    sharedViewModel.updateBlockedAppsInterfaz(emptyList())
                    Log.e("BlockedAppsFragment", "Todos Apps delited")
                }
            }
        }

        binding.llenarAllAppBoton.setOnClickListener {
            lifecycleScope.launch { sharedViewModel.updateBDApps(requireContext().packageManager) }
        }

    }

    private fun initObservers() {
        // 🔥 Observar cambios en la lista de apps bloqueadas
        sharedViewModel.blockedApps.observe(viewLifecycleOwner) { newList ->
            blockedAppAdapter.updateListEnAdaptador(newList)

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

    private fun initUI(view: View) {
        _binding = FragmentBlockedAppsBinding.bind(view)

        blockedAppAdapter = BlockedAppsAdapter(mutableListOf(), requireContext())
        binding.rvAppsBloqueadas.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAppsBloqueadas.adapter = blockedAppAdapter
        binding.rvAppsBloqueadas.setRecycledViewPool(RecyclerView.RecycledViewPool()) // ✅ Optimización

    }


    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { sharedViewModel.updateBDApps(requireContext().packageManager) }
        lifecycleScope.launch { sharedViewModel.loadBlockedAppsDeBDaViewModel() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 🔥 Evitar memory leaks
    }
}
