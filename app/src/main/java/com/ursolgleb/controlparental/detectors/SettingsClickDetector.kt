package com.ursolgleb.controlparental.detectors

import android.view.accessibility.AccessibilityEvent
import com.ursolgleb.controlparental.checkers.AppBlockChecker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsClickDetector @Inject constructor(
    private val appBlockChecker: AppBlockChecker
) {
    private val blockedWords = listOf("app", "aplicac")

    fun shouldBlock(event: AccessibilityEvent, packageName: String): Boolean {
        return (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) &&
                packageName == "com.android.settings" &&
                appBlockChecker.shouldBlockByText(event, blockedWords)
    }
}
