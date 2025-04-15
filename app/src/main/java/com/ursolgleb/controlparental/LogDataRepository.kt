package com.ursolgleb.controlparental

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.DeadObjectException
import android.util.Log
import com.ursolgleb.controlparental.data.local.AppDatabase
import com.ursolgleb.controlparental.data.local.dao.AppDao
import com.ursolgleb.controlparental.data.local.dao.HorarioDao
import com.ursolgleb.controlparental.data.local.dao.UsageEventDao
import com.ursolgleb.controlparental.data.local.dao.UsageStatsDao
import com.ursolgleb.controlparental.data.local.entities.AppEntity
import com.ursolgleb.controlparental.data.local.entities.HorarioEntity
import com.ursolgleb.controlparental.data.local.entities.UsageEventEntity
import com.ursolgleb.controlparental.data.local.entities.UsageStatsEntity
import com.ursolgleb.controlparental.data.log.LogAppBlockerDatabase
import com.ursolgleb.controlparental.data.log.LogBlockedAppDao
import com.ursolgleb.controlparental.data.log.LogBlockedAppEntity
import com.ursolgleb.controlparental.utils.AppsFun
import com.ursolgleb.controlparental.utils.Fun
import com.ursolgleb.controlparental.utils.StatusApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogDataRepository @Inject constructor(
    val logDatabase: LogAppBlockerDatabase,
    @ApplicationContext val context: Context
) {

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    val logDao: LogBlockedAppDao = logDatabase.logBlockedAppDao()

    suspend fun saveLogBlockedApp(pkgName: String) {
        val existingApp = logDao.getLogBlockedApp(pkgName)
        if (existingApp != null) {
            val updatedApp = existingApp.copy(blockedAt = System.currentTimeMillis())
            logDao.updateLogBlockedApp(updatedApp)
        } else {
            logDao.insertLogBlockedApp(LogBlockedAppEntity(packageName = pkgName))
        }
    }


}
