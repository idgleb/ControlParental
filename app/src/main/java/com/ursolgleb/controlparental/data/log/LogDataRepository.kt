package com.ursolgleb.controlparental.data.log

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogDataRepository @Inject constructor(
    logDatabase: LogAppBlockerDatabase,
    @ApplicationContext val context: Context
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val logDao: LogBlockedAppDao = logDatabase.logBlockedAppDao()

    fun saveLogBlockedApp(pkgName: String) {
        scope.launch {
            val existingApp = logDao.getLogBlockedApp(pkgName)
            if (existingApp != null) {
                val updatedApp = existingApp.copy(blockedAt = System.currentTimeMillis())
                logDao.updateLogBlockedApp(updatedApp)
            } else {
                logDao.insertLogBlockedApp(LogBlockedAppEntity(packageName = pkgName))
            }
        }
    }


}
