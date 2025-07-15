package com.ursolgleb.controlparental.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ursolgleb.controlparental.data.auth.local.DeviceAuthLocalDataSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AuthStateReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "AuthStateReceiver"
        const val ACTION_AUTH_STATE_CHANGED = "com.ursolgleb.controlparental.AUTH_STATE_CHANGED"
        
        fun notifyAuthStateChanged(context: Context) {
            val intent = Intent(ACTION_AUTH_STATE_CHANGED)
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        }
    }
    
    @Inject
    lateinit var deviceAuthLocalDataSource: DeviceAuthLocalDataSource
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_AUTH_STATE_CHANGED) return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val hasToken = deviceAuthLocalDataSource.getApiToken() != null
                
                if (hasToken) {
                    Log.d(TAG, "Token found, starting LocationWatcherService")
                    // Assuming LocationWatcherService is the intended service to start
                    // You would need to start it here if it's a background service
                    // For now, just logging the action
                } else {
                    Log.d(TAG, "No token found, stopping LocationWatcherService")
                    // Assuming LocationWatcherService is the intended service to stop
                    // You would need to stop it here if it's a background service
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking auth state", e)
            }
        }
    }
} 