package com.myperfectoutfit.data.remote

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.myperfectoutfit.BuildConfig
import com.myperfectoutfit.data.local.entities.*
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AzureOutfitService @Inject constructor() {

    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateOutfitRecommendation(
        shirts: List<ShirtEntity>,
        pants: List<PantEntity>,
        shoes: List<ShoeEntity>,
        ties: List<TieEntity>,
        watches: List<WatchEntity>,
        fragrances: List<FragranceEntity>,
        jackets: List<JacketEntity>,
        bags: List<BagEntity>,
        dresses: List<DressEntity>,
        skirts: List<SkirtEntity>,
        customGarments: List<CustomGarmentEntity> = emptyList(),
        styleRules: List<StyleRuleEntity> = emptyList(),
        baseGarments: List<Any> = emptyList(),
        userInstruction: String = "",
        excludeIds: List<String> = emptyList()
    ): String = suspendCancellableCoroutine { continuation ->
        
        val baseUrl = BuildConfig.AZURE_AI_ENDPOINT.trim().removeSuffix("/")
        val url = "$baseUrl/chat/completions"
        val apiKey = BuildConfig.AZURE_AI_KEY.trim()
        val deployment = BuildConfig.AZURE_AI_DEPLOYMENT
        
        val deviceLocale = Locale.getDefault()
        val languageName = deviceLocale.displayLanguage
        val currentDate = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", deviceLocale).format(Date())

        val inventorySections = mutableListOf<String>()
        if (shirts.isNotEmpty()) {
            inventorySections.add("--- CAMISAS/TOPS ---\n" + shirts.joinToString("\n") { 
                "- ID:${it.id} | ${it.brand} | ${it.subType} | Color: ${it.primaryColor} | Patrón: ${it.pattern} | Manga: ${it.sleeveLength} | Cuello: ${it.necklineStyle} | Material: ${it.material} | Corte: ${it.fit} | Formalidad: ${it.formalityLevel}" 
            })
        }
        if (pants.isNotEmpty()) {
            inventorySections.add("--- PANTALONES ---\n" + pants.joinToString("\n") { 
                "- ID:${it.id} | ${it.brand ?: "S/M"} | ${it.subType} | Color: ${it.primaryColor} | Material: ${it.material} | Largo: ${it.lengthStyle} | Tiro: ${it.waistRise} | Corte: ${it.fitStyle} | Formalidad: ${it.formalityLevel}" 
            })
        }
        if (shoes.isNotEmpty()) {
            inventorySections.add("--- CALZADO ---\n" + shoes.joinToString("\n") { 
                "- ID:${it.id} | ${it.brand} | ${it.subType} ${it.style} | Color: ${it.color} | Material: ${it.material} | Tacón: ${it.heelHeightStyle} | Punta: ${it.toeStyle} | Cierre: ${it.closureType} | Formalidad: ${it.formalityLevel}" 
            })
        }
        if (jackets.isNotEmpty()) {
            inventorySections.add("--- CHAQUETAS ---\n" + jackets.joinToString("\n") { "- ID:${it.id} | ${it.brand ?: "S/M"} | ${it.color} | Tipo: ${it.type} | Cierre: ${it.closureType}" })
        }
        if (bags.isNotEmpty()) {
            inventorySections.add("--- BOLSOS ---\n" + bags.joinToString("\n") { "- ID:${it.id} | ${it.brand ?: "S/M"} | ${it.color} | Estilo: ${it.style} | Tamaño: ${it.size}" })
        }
        if (ties.isNotEmpty()) {
            inventorySections.add("--- CORBATAS ---\n" + ties.joinToString("\n") { "- ID:${it.id} | Color: ${it.colorRange} | Diseño: ${it.pattern} | Material: ${it.material}" })
        }
        if (watches.isNotEmpty()) {
            inventorySections.add("--- RELOJES ---\n" + watches.joinToString("\n") { "- ID:${it.id} | ${it.brand} ${it.model} | Color: ${it.dialColor} | Correa: ${it.strapColor} (${it.strapMaterial})" })
        }
        if (fragrances.isNotEmpty()) {
            inventorySections.add("--- FRAGANCIAS ---\n" + fragrances.joinToString("\n") { "- ID:${it.id} | ${it.brand} ${it.name} | Perfil: ${it.profile} | Ocasión: ${it.occasionTag}" })
        }
        if (dresses.isNotEmpty()) {
            inventorySections.add("--- VESTIDOS ---\n" + dresses.joinToString("\n") { "- ID:${it.id} | ${it.brand ?: "S/M"} | ${it.color} | Patrón: ${it.pattern} | Largo: ${it.length} | Manga: ${it.sleeveStyle} | Material: ${it.material}" })
        }
        if (skirts.isNotEmpty()) {
            inventorySections.add("--- FALDAS ---\n" + skirts.joinToString("\n") { "- ID:${it.id} | ${it.brand ?: "S/M"} | ${it.color} | Estilo: ${it.style} | Largo: ${it.length} | Material: ${it.material}" })
        }
        if (customGarments.isNotEmpty()) {
            inventorySections.add("--- PRENDAS PERSONALIZADAS ---\n" + customGarments.joinToString("\n") { "- ID:${it.id} | ${it.attributeValues.replace("|", " - ")}" })
        }

        val basePrompt = if (baseGarments.isNotEmpty()) {
            "\nDEBES INCLUIR OBLIGATORIAMENTE ESTOS IDs:\n" + baseGarments.joinToString("\n") { item ->
                when (item) {
                    is ShirtEntity -> "- Camisa ID:${item.id}"
                    is PantEntity -> "- Pantalón ID:${item.id}"
                    is ShoeEntity -> "- Calzado ID:${item.id}"
                    is CustomGarmentEntity -> "- Prenda ID:${item.id}"
                    else -> "- ID: desconcocido"
                }
            }
        } else ""

        val rulesPrompt = if (styleRules.isNotEmpty()) "\nREGLAS DE ESTILO DEL USUARIO:\n" + styleRules.joinToString("\n") { "- ${it.title}: ${it.description}" } else ""
        val avoidPrompt = if (excludeIds.isNotEmpty()) "\nEVITA ESTOS IDs (ya usados recientemente): ${excludeIds.joinToString()}" else ""

        val systemInstruction = """
            Eres un Asesor de Imagen y Estilista Personal experto.
            Tu objetivo es crear outfits impecables usando SOLO el inventario proporcionado.
            REGLAS DE ORO:
            1. ARMONÍA: Aplica teoría del color (complementarios, análogos).
            2. OCASIÓN: Selecciona el calzado acorde a la formalidad de la camisa y pantalón.
            3. TEXTURAS: No mezcles materiales que choquen visualmente.
            4. IDIOMA: Responde SIEMPRE en $languageName elegante y natural.
            5. FORMATO: NO uses Markdown (negritas, listas, etc.). Texto plano elegante.
        """.trimIndent()

        val userPrompt = """
            HOY ES: $currentDate
            $rulesPrompt
            $avoidPrompt
            $basePrompt
            PETICIÓN DEL USUARIO: $userInstruction

            INVENTARIO DISPONIBLE:
            ${inventorySections.joinToString("\n\n")}

            INSTRUCCIONES DE RESPUESTA:
            1. Describe el outfit y por qué es una excelente elección.
            2. Sé persuasivo y educado.
            3. Responde estrictamente en idioma $languageName.
            4. PROHIBIDO: No menciones los códigos de ID (ej: ID:123) dentro de la descripción narrativa. Los IDs solo deben ir en la línea técnica final.
            5. CRÍTICO: Al final, incluye la línea: SELECCION_IDS: SHIRT=id, PANT=id, SHOE=id, TIE=id, WATCH=id, FRAGRANCE=id, JACKET=id, BAG=id, DRESS=id, SKIRT=id
               (Sustituye 'id' por el número real. Usa 'null' si no seleccionas nada de esa categoría).
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            put("model", deployment)
            put("messages", JSONArray().apply {
                // Combinamos instrucciones en un solo mensaje de usuario para máxima compatibilidad con modelos mini/reasoning
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "$systemInstruction\n\n$userPrompt")
                })
            })
            put("max_completion_tokens", 2000)
            put("reasoning_effort", "medium") 
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("api-key", apiKey)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume("Error de red: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d("AzureRaw", "Body: $body")
                if (response.isSuccessful && body != null) {
                    try {
                        val json = JSONObject(body)
                        val choices = json.getJSONArray("choices")
                        val message = choices.getJSONObject(0).getJSONObject("message")
                        val content = message.optString("content", "")
                        
                        Log.d("AzureResponse", "Outfit content: $content")
                        continuation.resume(content)
                    } catch (e: Exception) {
                        Log.e("AzureReset", "Parsing error: ${e.message}")
                        continuation.resume("Error al interpretar la respuesta de la IA.")
                    }
                } else {
                    Log.e("AzureReset", "Error ${response.code}: $body")
                    continuation.resume("Error Azure ${response.code}. Verifica créditos o configuración.")
                }
            }
        })
    }

    suspend fun analyzeGarmentImage(
        bitmap: Bitmap, 
        categoryName: String? = null,
        customAttributes: String? = null
    ): String? = suspendCancellableCoroutine { continuation ->
        
        val baseUrl = BuildConfig.AZURE_AI_ENDPOINT.trim().removeSuffix("/")
        val url = "$baseUrl/chat/completions"
        val base64Image = bitmapToBase64(bitmap)
        
        val deviceLocale = Locale.getDefault()
        val languageName = deviceLocale.displayLanguage
        
        val detectPrompt = if (customAttributes != null) "Detecta estos campos personalizados: $customAttributes." else "Detecta: subType (tipo de prenda), brand (marca), color (tonalidad exacta), material (tela), pattern (estampado)."
        
        val systemMessage = """
            Eres un analista de moda experto en visión artificial.
            Tu tarea es identificar con precisión los detalles de una prenda a partir de una foto.
            REGLAS:
            1. Si no ves marca, usa 'Sin Marca'.
            2. Si no estás seguro del material, indica el más probable.
            3. Formato de respuesta: JSON puro, sin bloques de código ni texto adicional.
            4. IMPORTANTE: Todos los valores de los campos JSON deben estar en idioma $languageName.
        """.trimIndent()

        val userPromptText = "Analiza esta prenda ${categoryName ?: ""}. $detectPrompt"

        val jsonBody = JSONObject().apply {
            put("model", BuildConfig.AZURE_AI_DEPLOYMENT)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", "$systemMessage\n\n$userPromptText")
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,$base64Image")
                            })
                        })
                    })
                })
            })
            put("max_completion_tokens", 1500)
            put("reasoning_effort", "medium")
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("api-key", BuildConfig.AZURE_AI_KEY.trim())
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        val call = client.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resume(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val content = JSONObject(body)
                            .getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                        
                        Log.d("AzureResponse", "Outfit content: $content")
                        continuation.resume(content)
                    } catch (_: Exception) {
                        continuation.resume(null)
                    }
                } else {
                    Log.e("AzureReset", "Error Vision ${response.code}: $body")
                    continuation.resume(null)
                }
            }
        })
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
