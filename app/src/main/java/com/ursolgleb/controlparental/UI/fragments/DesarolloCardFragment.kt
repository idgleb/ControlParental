package com.ursolgleb.controlparental.UI.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import com.ursolgleb.controlparental.R
import androidx.lifecycle.lifecycleScope
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.data.apps.dao.AppDao
import com.ursolgleb.controlparental.databinding.FragmentDesarolloCardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class DesarolloCardFragment : Fragment(R.layout.fragment_desarollo_card) {

    @Inject
    lateinit var appDataRepository: AppDataRepository

    lateinit var appDao: AppDao

    private var _binding: FragmentDesarolloCardBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appDao = appDataRepository.appDatabase.appDao()

        initUI(view)

        initListeners()

        initObservers()
    }

    private fun initListeners() {

        binding.delitBlackedAppBoton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                appDao.unblockAllApps()
                withContext(Dispatchers.Main) {
                    Log.w("BlockedAppsFragment", "Apps delited")
                }
            }
        }

        binding.delitAllAppBoton.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                appDao.deleteAllApps()
                withContext(Dispatchers.Main) {
                    Log.e("BlockedAppsFragment", "Todos Apps delited")
                }
            }
        }

        binding.llenarAllAppBoton.setOnClickListener {
            appDataRepository.updateBDApps()
        }

        binding.testBoton.setOnClickListener {
/*            val pkgName = "com.ursolgleb.controlparental"
            Log.e("MioParametro", "testBoton")
            val listPkgName = listOf(pkgName)
            val tiempoDeUso = appDataRepository.getTiempoDeUsoSeconds(listPkgName) { app -> app }
            Log.e("MioParametro", "getTiempoDeUsoSeconds $pkgName: $tiempoDeUso")*/


            viewLifecycleOwner.lifecycleScope.launch {
                //appDataRepository.XXX
            }


        }

    }

    private fun initObservers() {
        //
    }

    private fun initUI(view: View) {
        _binding = FragmentDesarolloCardBinding.bind(view)
     //
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.e("DesarolloCardFragment", "onDestroyView")
        _binding = null // 🔥 Evitar memory leaks
    }
}
