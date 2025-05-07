package com.ursolgleb.controlparental.checkers

import android.view.accessibility.AccessibilityEvent
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.utils.AppsFun
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppBlockChecker @Inject constructor(
    private val appDataRepository: AppDataRepository
) {

    fun isAppBlocked(packageName: String): Boolean {
        return appDataRepository.blockedAppsFlow.value.any { it.packageName == packageName }
    }

    suspend fun isNewAppWithUi(packageName: String): Boolean {
        return appDataRepository.siEsNuevoPkg(packageName) &&
                AppsFun.siTieneUI(appDataRepository.context, packageName)
    }

    fun shouldBlockByText(event: AccessibilityEvent, palabrasBloqueadas: List<String>): Boolean {
        val text = event.text.joinToString(", ")
        return palabrasBloqueadas.any { palabra -> text.contains(palabra, ignoreCase = true) }
    }
}
