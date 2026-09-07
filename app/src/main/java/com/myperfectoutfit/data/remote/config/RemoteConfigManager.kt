package com.myperfectoutfit.data.remote.config

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.myperfectoutfit.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteConfigManager @Inject constructor() {

    private val remoteConfig = Firebase.remoteConfig

    init {
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 3600 // 1 hora
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // Valores por defecto (desde local.properties para pruebas)
        val defaults = mapOf(
            "AZURE_AI_KEY" to BuildConfig.AZURE_AI_KEY,
            "AZURE_AI_ENDPOINT" to BuildConfig.AZURE_AI_ENDPOINT,
            "AZURE_AI_DEPLOYMENT" to BuildConfig.AZURE_AI_DEPLOYMENT
        )
        remoteConfig.setDefaultsAsync(defaults)
        
        fetchAndActivate()
    }

    fun fetchAndActivate() {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    android.util.Log.d("RemoteConfig", "Configuración actualizada: $updated")
                } else {
                    android.util.Log.e("RemoteConfig", "Error al descargar configuración")
                }
            }
    }

    fun getAzureKey(): String = remoteConfig.getString("AZURE_AI_KEY")
    fun getAzureEndpoint(): String = remoteConfig.getString("AZURE_AI_ENDPOINT")
    fun getAzureDeployment(): String = remoteConfig.getString("AZURE_AI_DEPLOYMENT")
}
