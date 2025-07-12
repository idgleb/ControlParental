package com.ursolgleb.controlparental.data.auth.local

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.content.Context
import androidx.work.WorkManager
import com.ursolgleb.controlparental.domain.auth.model.DeviceToken
import com.ursolgleb.controlparental.data.local.dao.DeviceDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import com.ursolgleb.controlparental.receivers.AuthStateReceiver
import androidx.core.content.edit

/**
 * Fuente de datos local para la autenticación del dispositivo
 * Usa SharedPreferences encriptadas para mayor seguridad
 */
@Singleton
class DeviceAuthLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceDao: DeviceDao
) {
    companion object {
        private const val TAG = "DeviceAuthLocalDataSource"
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
    fun saveDeviceId(deviceId: String): Boolean {
        val result = encryptedPrefs.edit().apply {
            putString(KEY_DEVICE_ID, deviceId)
            putBoolean(KEY_IS_REGISTERED, true)
        }.commit()
        if (result) {
            android.util.Log.d(TAG, "DeviceId guardado exitosamente: $deviceId")
        }
        updateAuthState()
        return result
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
    fun saveApiToken(token: DeviceToken): Boolean {
        val currentDeviceId = getDeviceId()
        // Solo sobrescribir si el deviceId es igual al guardado o si no hay ninguno guardado
        val shouldOverwriteDeviceId = currentDeviceId == null || currentDeviceId == token.deviceId
        val editor = encryptedPrefs.edit()
        editor.putString(KEY_API_TOKEN, token.token)
        if (shouldOverwriteDeviceId && token.deviceId != null) {
            editor.putString(KEY_DEVICE_ID, token.deviceId)
        }
        editor.putBoolean(KEY_IS_VERIFIED, true)
        val result = editor.commit()
        if (result) {
            android.util.Log.d(TAG, "Token guardado exitosamente: "+token.token.take(8)+"..., deviceId: "+token.deviceId)
        }
        updateAuthState()
        return result
    }
    
    /**
     * Obtener token guardado
     */
    fun getApiToken(): DeviceToken? {
        val token = encryptedPrefs.getString(KEY_API_TOKEN, null)
        val deviceId = encryptedPrefs.getString(KEY_DEVICE_ID, null)
        
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
        encryptedPrefs.edit() { clear() }
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
        // Notificar al receiver sobre el cambio de estado
        AuthStateReceiver.notifyAuthStateChanged(context)
    }
    
    /**
     * Limpia solo el token de autenticación, mantiene el registro del dispositivo
     * Usado cuando el token expira o es inválido
     */
    suspend fun clearToken() {
        withContext(Dispatchers.IO) {
            val result = encryptedPrefs.edit().apply {
                remove(KEY_API_TOKEN)
                putBoolean(KEY_IS_VERIFIED, false)
                // Mantener KEY_DEVICE_ID y KEY_IS_REGISTERED
            }.commit()
            if (!result) {
                android.util.Log.e(TAG, "clearToken: Error al eliminar el token de autenticación")
            }
            android.util.Log.d("DeviceAuthLocalDataSource", "clearToken: Token eliminado, dispositivo sigue registrado")
            
            // Cancelar el worker de sincronización
            try {
                WorkManager.getInstance(context).cancelUniqueWork("ModernSyncWorker")
                android.util.Log.d("DeviceAuthLocalDataSource", "clearToken: ModernSyncWorker cancelado")
            } catch (e: Exception) {
                android.util.Log.e("DeviceAuthLocalDataSource", "clearToken: Error cancelando ModernSyncWorker", e)
            }
            
            updateAuthState()
        }
    }
    
    /**
     * Limpia el registro del dispositivo pero mantiene el deviceId
     * Usado cuando el dispositivo es eliminado del servidor pero queremos mantener datos locales
     */
    suspend fun clearRegistration() {
        withContext(Dispatchers.IO) {
            val result = encryptedPrefs.edit().apply {
                remove(KEY_API_TOKEN)
                putBoolean(KEY_IS_VERIFIED, false)
                putBoolean(KEY_IS_REGISTERED, false)
                // Mantener KEY_DEVICE_ID para preservar datos locales
            }.commit()
            if (!result) {
                android.util.Log.e(TAG, "clearRegistration: Error al eliminar el registro del dispositivo")
            }
            android.util.Log.d("DeviceAuthLocalDataSource", "clearRegistration: Registro eliminado, deviceId mantenido")
            
            // Cancelar el worker de sincronización
            try {
                WorkManager.getInstance(context).cancelUniqueWork("ModernSyncWorker")
                android.util.Log.d("DeviceAuthLocalDataSource", "clearRegistration: ModernSyncWorker cancelado")
            } catch (e: Exception) {
                android.util.Log.e("DeviceAuthLocalDataSource", "clearRegistration: Error cancelando ModernSyncWorker", e)
            }
            
            updateAuthState()
        }
    }
    
    /**
     * Alias para clearRegistration() - mantener por compatibilidad
     */
    suspend fun clearAll() {
        clearRegistration()
    }
    
    /**
     * Limpia absolutamente todo, incluyendo el deviceId
     * Usar solo cuando se quiere un reset completo de la app
     */
    suspend fun clearEverything() {
        withContext(Dispatchers.IO) {
            val result = encryptedPrefs.edit().clear().commit()
            if (!result) {
                android.util.Log.e(TAG, "clearEverything: Error al limpiar todas las credenciales")
            }
            
            // Limpiar toda la base de datos
            try {
                deviceDao.deleteAll()
                android.util.Log.d("DeviceAuthLocalDataSource", "clearEverything: Todo eliminado incluyendo deviceId")
            } catch (e: Exception) {
                android.util.Log.e("DeviceAuthLocalDataSource", "clearEverything: Error limpiando devices", e)
            }
            
            // Cancelar el worker
            try {
                WorkManager.getInstance(context).cancelUniqueWork("ModernSyncWorker")
            } catch (e: Exception) {
                android.util.Log.e("DeviceAuthLocalDataSource", "clearEverything: Error cancelando ModernSyncWorker", e)
            }
            
            updateAuthState()
        }
    }
} 