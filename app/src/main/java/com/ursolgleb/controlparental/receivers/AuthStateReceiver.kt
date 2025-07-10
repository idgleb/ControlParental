package com.ursolgleb.controlparental.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ursolgleb.controlparental.data.auth.local.DeviceAuthLocalDataSource
import com.ursolgleb.controlparental.services.HeartbeatService
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
        
        Log.d(TAG, "Auth state changed, checking if HeartbeatService should run")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val hasToken = deviceAuthLocalDataSource.getApiToken() != null
                
                if (hasToken) {
                    Log.d(TAG, "Token found, starting HeartbeatService")
                    HeartbeatService.start(context)
                } else {
                    Log.d(TAG, "No token found, stopping HeartbeatService")
                    HeartbeatService.stop(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking auth state", e)
            }
        }
    }
} 