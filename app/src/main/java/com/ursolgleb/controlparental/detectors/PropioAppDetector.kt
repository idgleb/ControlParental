package com.ursolgleb.controlparental.detectors

import android.content.Context
import android.util.Log
import com.ursolgleb.controlparental.R
import android.view.accessibility.AccessibilityEvent
import com.ursolgleb.controlparental.checkers.AppBlockChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PropioAppDetector @Inject constructor(
    private val appBlockChecker: AppBlockChecker,
    @ApplicationContext context: Context
) {

    private val blockedWords = listOf( context.getString(R.string.app_name) )

    fun shouldBlock(event: AccessibilityEvent): Boolean {
        return (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                event.eventType == AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT ||
                event.eventType == AccessibilityEvent.TYPE_ANNOUNCEMENT) &&
                appBlockChecker.shouldBlockByText(event, blockedWords)
    }
}
