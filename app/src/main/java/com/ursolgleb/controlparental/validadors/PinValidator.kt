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
class PinValidator @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,                       // 1º  → Context
            "parent_prefs",                // 2º  → fileName
            masterKey,                     // 3º  → MasterKey
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }


    /** Guarda hash del PIN (llámalo durante el onboarding del padre) */
    fun savePin(pin: String) = prefs.edit() {
        putString("hash", hash(pin))
    }

    /** Comprueba que el PIN introducido coincide con el hash */
    fun isPinCorrect(pin: String): Boolean =
        hash(pin) == prefs.getString("hash", null)

    private fun hash(pin: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray())
            .joinToString("") { "%02x".format(it) }
}

