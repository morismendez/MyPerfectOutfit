package com.myperfectoutfit.data.local.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurePreferences @Inject constructor(@ApplicationContext context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveGeminiApiKey(apiKey: String) {
        sharedPreferences.edit().putString("gemini_api_key", apiKey).apply()
    }

    fun getGeminiApiKey(): String? {
        return sharedPreferences.getString("gemini_api_key", null)
    }

    fun clearGeminiApiKey() {
        sharedPreferences.edit().remove("gemini_api_key").apply()
    }
}
