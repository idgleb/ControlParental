package com.ursolgleb.controlparental.detectors

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import com.ursolgleb.controlparental.R
import com.ursolgleb.controlparental.checkers.AppBlockChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsClickDetector @Inject constructor(
    private val appBlockChecker: AppBlockChecker,
    @ApplicationContext context: Context
) {
    private val blockedWords = listOf("app", "aplicac", context.getString(R.string.app_name))

    fun shouldBlock(event: AccessibilityEvent, packageName: String): Boolean {
        return (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) &&
                packageName == "com.android.settings" &&
                appBlockChecker.shouldBlockByText(event, blockedWords)
    }
}
