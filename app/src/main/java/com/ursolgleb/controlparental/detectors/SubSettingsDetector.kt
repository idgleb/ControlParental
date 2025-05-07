package com.ursolgleb.controlparental.detectors

import android.view.accessibility.AccessibilityEvent
import com.ursolgleb.controlparental.checkers.AppBlockChecker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubSettingsDetector @Inject constructor(
    private val appBlockChecker: AppBlockChecker
) {
    private val blockedWords = listOf("app")

    fun shouldBlock(event: AccessibilityEvent): Boolean {
        return event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                event.className?.toString() == "com.android.settings.SubSettings" &&
                appBlockChecker.shouldBlockByText(event, blockedWords)
    }
}
