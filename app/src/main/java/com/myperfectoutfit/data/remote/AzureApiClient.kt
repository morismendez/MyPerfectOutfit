package com.myperfectoutfit.data.remote

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.myperfectoutfit.BuildConfig
import com.myperfectoutfit.data.remote.config.RemoteConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AzureApiClient @Inject constructor(
    private val remoteConfigManager: RemoteConfigManager
) {

    private val gson = Gson()
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    fun getApiKey(): String = remoteConfigManager.getAzureKey().ifBlank { BuildConfig.AZURE_AI_KEY }
    fun getEndpoint(): String = remoteConfigManager.getAzureEndpoint().ifBlank { BuildConfig.AZURE_AI_ENDPOINT }
    fun getDeployment(): String = remoteConfigManager.getAzureDeployment().ifBlank { BuildConfig.AZURE_AI_DEPLOYMENT }

    fun hasLocalConfig(): Boolean {
        return getApiKey().isNotBlank() && getEndpoint().isNotBlank()
    }

    fun buildUrl(endpoint: String, deployment: String): String {
        val trimmed = endpoint.trim()
        val qIndex = trimmed.indexOf("?")
        var path = if (qIndex != -1) trimmed.substring(0, qIndex) else trimmed
        var query = if (qIndex != -1) trimmed.substring(qIndex + 1) else ""

        if (path.endsWith("/")) {
            path = path.dropLast(1)
        }

        if (!path.contains("/chat/completions")) {
            path = if (path.contains(".openai.azure.com") && !path.contains("/openai/deployments/")) {
                val dep = deployment.ifBlank { "gpt-4o" }
                "$path/openai/deployments/$dep/chat/completions"
            } else {
                "$path/chat/completions"
            }
        }

        if (path.contains(".openai.azure.com")) {
            if (!query.contains("api-version=")) {
                val sep = if (query.isNotBlank()) "&" else ""
                query = "$query${sep}api-version=2024-08-01-preview"
            }
        }

        return if (query.isNotBlank()) "$path?$query" else path
    }

    suspend fun callDirectly(
        systemInstruction: String,
        userPrompt: String,
        imageUrl: String? = null
    ): String = withContext(Dispatchers.IO) {
        val key = getApiKey()
        val rawEndpoint = getEndpoint()
        val deployment = getDeployment()

        if (key.isBlank() || rawEndpoint.isBlank()) {
            throw IllegalStateException("Configuración de Azure AI no encontrada (Key o Endpoint vacíos)")
        }

        val url = buildUrl(rawEndpoint, deployment)
        Log.d("AzureApiClient", "Llamando directamente a Azure AI Endpoint: $url")

        val messages = mutableListOf<Map<String, Any>>()
        if (systemInstruction.isNotBlank()) {
            messages.add(mapOf("role" to "system", "content" to systemInstruction))
        }

        if (!imageUrl.isNullOrBlank()) {
            messages.add(
                mapOf(
                    "role" to "user",
                    "content" to listOf(
                        mapOf("type" to "text", "text" to userPrompt),
                        mapOf("type" to "image_url", "image_url" to mapOf("url" to imageUrl))
                    )
                )
            )
        } else {
            messages.add(mapOf("role" to "user", "content" to userPrompt))
        }

        val requestMap = mapOf(
            "model" to deployment,
            "messages" to messages,
            "max_tokens" to if (!imageUrl.isNullOrBlank()) 1500 else 2500
        )

        val jsonBody = gson.toJson(requestMap)
        val request = Request.Builder()
            .url(url)
            .addHeader("api-key", key)
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e("AzureApiClient", "Error HTTP [${response.code}] llamando a Azure: $responseBody")
                throw IOException("Error en el servidor de IA (${response.code})")
            }

            val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
            val choices = jsonObject?.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                val message = choices.get(0).asJsonObject.getAsJsonObject("message")
                message.get("content").asString
            } else {
                throw IOException("Respuesta vacía de Azure AI")
            }
        }
    }
}
