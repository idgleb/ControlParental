package com.ursolgleb.controlparental.UI.viewmodel

import android.app.Application
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.DeadObjectException
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ursolgleb.controlparental.AppDataRepository
import com.ursolgleb.controlparental.data.local.AppDatabase
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.BlockedEntity
import com.ursolgleb.controlparental.utils.Fun
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class SharedViewModel @Inject constructor(
    private val appDataRepository: AppDataRepository,
    application: Application
) : AndroidViewModel(application) {

    private val appDao = appDataRepository.appDatabase.appDao()

    private val mutex_updateBDApps = Mutex()
    private val _mutexUpdateBDAppsState = MutableStateFlow(false)
    val mutexUpdateBDAppsState: StateFlow<Boolean> = _mutexUpdateBDAppsState

    private val mutex_inicieDelecturaDeBD = Mutex()

    private val mutex_Global = Mutex() // Mutex compartido para sincronización

    private val _todosApps = MutableStateFlow<List<AppEntity>>(emptyList())
    val todosApps: StateFlow<List<AppEntity>> = _todosApps

    private val _blockedApps = MutableStateFlow<List<AppEntity>>(emptyList())
    val blockedApps: StateFlow<List<AppEntity>> = _blockedApps

    private val _todosAppsMenosBlaqueados = MutableStateFlow<List<AppEntity>>(emptyList())
    val todosAppsMenosBlaqueados: StateFlow<List<AppEntity>> = _todosAppsMenosBlaqueados

    var inicieDeLecturaDeBD = false

    init {
        Log.w("SharedViewModel3", "init SharedViewModel $inicieDeLecturaDeBD")
        observarDatosDesdeRepositorio()
    }

    private fun observarDatosDesdeRepositorio() {
        viewModelScope.launch {
            appDataRepository.todosAppsFlow.collect { apps ->
                _todosApps.value = apps
            }
        }

        viewModelScope.launch {
            appDataRepository.blockedAppsFlow.collect { blockedApps ->
                _blockedApps.value = blockedApps
            }
        }

        viewModelScope.launch {
            appDataRepository.todosAppsMenosBloqueadosFlow.collect { appsNoBloqueados ->
                _todosAppsMenosBlaqueados.value = appsNoBloqueados
            }
        }
    }



}

