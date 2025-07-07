package com.ursolgleb.controlparental.data.auth.local

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ursolgleb.controlparental.domain.auth.model.DeviceToken
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source local para manejo seguro de credenciales
 */
@Singleton
class DeviceAuthLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val PREFS_NAME = "device_auth_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_API_TOKEN = "api_token"
        private const val KEY_IS_REGISTERED = "is_registered"
        private const val KEY_IS_VERIFIED = "is_verified"
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val _authStateFlow = MutableStateFlow(getInitialAuthState())
    val authStateFlow: Flow<Boolean> = _authStateFlow.asStateFlow()
    
    /**
     * Guardar ID del dispositivo
     */
    fun saveDeviceId(deviceId: String) {
        encryptedPrefs.edit().apply {
            putString(KEY_DEVICE_ID, deviceId)
            putBoolean(KEY_IS_REGISTERED, true)
            apply()
        }
        updateAuthState()
    }
    
    /**
     * Obtener ID del dispositivo
     */
    fun getDeviceId(): String? {
        return encryptedPrefs.getString(KEY_DEVICE_ID, null)
    }
    
    /**
     * Guardar token de API
     */
    fun saveApiToken(token: DeviceToken) {
        Log.d("DeviceAuthLocalDataSource", "saveApiToken: Guardando token=${token.token.take(10)}... para deviceId=${token.deviceId}")
        encryptedPrefs.edit().apply {
            putString(KEY_API_TOKEN, token.token)
            putString(KEY_DEVICE_ID, token.deviceId)
            putBoolean(KEY_IS_VERIFIED, true)
            apply()
        }
        Log.d("DeviceAuthLocalDataSource", "saveApiToken: Token guardado en SharedPreferences")
        updateAuthState()
    }
    
    /**
     * Obtener token guardado
     */
    fun getApiToken(): DeviceToken? {
        val token = encryptedPrefs.getString(KEY_API_TOKEN, null)
        val deviceId = encryptedPrefs.getString(KEY_DEVICE_ID, null)
        
        Log.d("DeviceAuthLocalDataSource", "getApiToken: token=${token?.take(10)}..., deviceId=$deviceId")
        
        return if (token != null && deviceId != null) {
            DeviceToken(token = token, deviceId = deviceId)
        } else {
            null
        }
    }
    
    /**
     * Limpiar todas las credenciales
     */
    fun clearCredentials() {
        encryptedPrefs.edit().clear().apply()
        updateAuthState()
    }
    
    /**
     * Verificar si el dispositivo está registrado
     */
    fun isDeviceRegistered(): Boolean {
        return encryptedPrefs.getBoolean(KEY_IS_REGISTERED, false)
    }
    
    /**
     * Verificar si el dispositivo está verificado
     */
    fun isDeviceVerified(): Boolean {
        return encryptedPrefs.getBoolean(KEY_IS_VERIFIED, false)
    }
    
    private fun getInitialAuthState(): Boolean {
        return isDeviceVerified() && getApiToken() != null
    }
    
    private fun updateAuthState() {
        _authStateFlow.value = getInitialAuthState()
    }
} 