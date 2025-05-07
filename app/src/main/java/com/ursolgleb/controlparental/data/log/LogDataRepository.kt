package com.ursolgleb.controlparental.data.log

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
