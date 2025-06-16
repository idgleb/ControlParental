package com.ursolgleb.controlparental.validadors


import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class PinValidator @Inject constructor(@ApplicationContext private val context: Context) {

    private var isAuthActivitiAbierta = false

    private val prefs: SharedPreferences by lazy {
        try {
            createEncryptedPrefs()
        } catch (_: Exception) {
            // If the preferences cannot be decrypted (e.g., after reinstalling
            // the app) clear the stored file and recreate it with a new key
            context.deleteSharedPreferences(PREF_NAME)
            createEncryptedPrefs()
        }
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,                       // 1º  → Context
            PREF_NAME,                     // 2º  → fileName
            masterKey,                     // 3º  → MasterKey
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val PREF_NAME = "parent_prefs"
    }

    fun isAuthActivitiAbierta(): Boolean {
        return isAuthActivitiAbierta
    }

    fun setAuthActivitiAbierta(abierta: Boolean) {
        isAuthActivitiAbierta = abierta
    }


    /** Guarda hash del PIN (llámalo durante el onboarding del padre) */
    fun savePin(pin: String) = prefs.edit() {
        putString("hash", hash(pin))
    }

    /** Devuelve true si ya se ha guardado un PIN previamente */
    fun isPinSet(): Boolean = prefs.contains("hash")

    /** Comprueba que el PIN introducido coincide con el hash */
    fun isPinCorrect(pin: String): Boolean =
        hash(pin) == prefs.getString("hash", null)

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }
}

