package com.myperfectoutfit.data.remote

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.myperfectoutfit.BuildConfig
import com.myperfectoutfit.data.local.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiOutfitService @Inject constructor() {

    private fun getModel(userApiKey: String?): GenerativeModel {
        val key = if (!userApiKey.isNullOrBlank()) userApiKey else BuildConfig.GEMINI_API_KEY
        return GenerativeModel(
            modelName = "gemini-3.6-flash", // Restaurado a la versión 3.6 confirmada en el historial
            apiKey = key,
        )
    }

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
        excludeIds: List<String> = emptyList(),
        userApiKey: String? = null
    ): String {
        val currentDate = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(Date())
        val generativeModel = getModel(userApiKey)

        val inventorySections = mutableListOf<String>()

        if (shirts.isNotEmpty()) {
            inventorySections.add("--- CAMISAS/TOPS ---\n" + shirts.joinToString("\n") { "- ID:${it.id} | ${it.subType} ${it.brand} | Color: ${it.primaryColor} ${if (it.secondaryColor != null) "+ ${it.secondaryColor}" else ""} | Patrón: ${it.pattern} | Manga: ${it.sleeveLength} | Cuello: ${it.necklineStyle} | Material: ${it.material} | Corte: ${it.fit} | Formalidad: ${it.formalityLevel}" })
        }
        if (pants.isNotEmpty()) {
            inventorySections.add("--- PANTALONES/INFERIORES ---\n" + pants.joinToString("\n") { "- ID:${it.id} | ${it.subType} ${it.brand ?: ""} | Color: ${it.primaryColor} | Material: ${it.material} | Largo: ${it.lengthStyle} | Tiro: ${it.waistRise} | Corte: ${it.fitStyle} | Formalidad: ${it.formalityLevel}" })
        }
        if (shoes.isNotEmpty()) {
            inventorySections.add("--- CALZADO ---\n" + shoes.joinToString("\n") { "- ID:${it.id} | ${it.subType} ${it.style} ${it.brand} | Color: ${it.color} | Material: ${it.material} | Tacón: ${it.heelHeightStyle} | Punta: ${it.toeStyle} | Cierre: ${it.closureType} | Formalidad: ${it.formalityLevel}" })
        }
        if (jackets.isNotEmpty()) {
            inventorySections.add("--- CHAQUETAS ---\n" + jackets.joinToString("\n") { "- ID:${it.id} | ${it.brand ?: ""} | ${it.color} | Tipo: ${it.type} | Cierre: ${it.closureType}" })
        }
        if (bags.isNotEmpty()) {
            inventorySections.add("--- BOLSOS ---\n" + bags.joinToString("\n") { "- ID:${it.id} | ${it.brand ?: ""} | ${it.color} | Estilo: ${it.style} | Tamaño: ${it.size}" })
        }
        if (ties.isNotEmpty()) {
            inventorySections.add("--- CORBATAS ---\n" + ties.joinToString("\n") { "- ID:${it.id} | ${it.colorRange} | Diseño: ${it.pattern}" })
        }
        if (watches.isNotEmpty()) {
            inventorySections.add("--- RELOJES ---\n" + watches.joinToString("\n") { "- ID:${it.id} | ${it.brand} ${it.model} | Correa: ${it.strapColor} (${it.strapMaterial})" })
        }
        if (fragrances.isNotEmpty()) {
            inventorySections.add("--- FRAGANCIAS ---\n" + fragrances.joinToString("\n") { "- ID:${it.id} | ${it.brand} ${it.name} | Perfil: ${it.profile} | Ocasión: ${it.occasionTag}" })
        }
        if (dresses.isNotEmpty()) {
            inventorySections.add("--- VESTIDOS ---\n" + dresses.joinToString("\n") { "- ID:${it.id} | ${it.brand ?: ""} | ${it.color} | Patrón: ${it.pattern} | Largo: ${it.length}" })
        }
        if (skirts.isNotEmpty()) {
            inventorySections.add("--- FALDAS ---\n" + skirts.joinToString("\n") { "- ID:${it.id} | ${it.brand ?: ""} | ${it.color} | Estilo: ${it.style} | Largo: ${it.length}" })
        }
        if (customGarments.isNotEmpty()) {
            inventorySections.add("--- PRENDAS PERSONALIZADAS ---\n" + customGarments.joinToString("\n") { "- ID:${it.id} | ${it.attributeValues.replace("|", " - ")}" })
        }

        val baseGarmentsPrompt = if (baseGarments.isNotEmpty()) {
            """
            
            PRENDAS BASE SELECCIONADAS (Debes incluirlas OBLIGATORIAMENTE en el outfit):
            ${baseGarments.joinToString("\n") { item ->
                when (item) {
                    is ShirtEntity -> "- Camisa/Top: ${item.brand} ${item.primaryColor} (${item.subType})"
                    is PantEntity -> "- Pantalón: ${item.brand} ${item.primaryColor} (${item.subType})"
                    is ShoeEntity -> "- Calzado: ${item.brand} ${item.color} (${item.style})"
                    is TieEntity -> "- Corbata: ${item.colorRange} ${item.pattern}"
                    is WatchEntity -> "- Reloj: ${item.brand} ${item.model}"
                    is FragranceEntity -> "- Fragancia: ${item.brand} ${item.name}"
                    is JacketEntity -> "- Chaqueta: ${item.brand} ${item.color}"
                    is BagEntity -> "- Bolso: ${item.brand} ${item.color}"
                    is DressEntity -> "- Vestido: ${item.brand} ${item.color}"
                    is SkirtEntity -> "- Falda: ${item.brand} ${item.color}"
                    is CustomGarmentEntity -> "- Prenda Personalizada: ${item.attributeValues.replace("|", " - ")}"
                    else -> "- Prenda seleccionada por el usuario"
                }
            }}
            """.trimIndent()
        } else ""

        val rulesPrompt = if (styleRules.isNotEmpty()) {
            """
            
            REGLAS DE ESTILO PERSONALIZADAS (Debes respetarlas estrictamente):
            ${styleRules.joinToString("\n") { "- ${it.title}: ${it.description}" }}
            """.trimIndent()
        } else ""

        val instructionPrompt = if (userInstruction.isNotBlank()) {
            """
            
            INSTRUCCIÓN Y PREFERENCIA DEL USUARIO PARA HOY:
            "$userInstruction"
            (Prioriza esta petición y construye el resto del outfit en torno a esta prenda o detalle).
            """.trimIndent()
        } else ""

        val avoidPrompt = if (excludeIds.isNotEmpty()) {
            """
            
            EVITA ESTAS PRENDAS (Ya sugeridas anteriormente):
            No utilices prendas con los siguientes IDs: ${excludeIds.joinToString(", ")}
            Por favor, intenta generar una propuesta DIFERENTE a las anteriores usando otros elementos de mi inventario.
            """.trimIndent()
        } else ""

        val prompt = """
            Eres un asesor de imagen y estilista personal experto con un gusto impecable. 
            Tu objetivo es generar una recomendación de outfit altamente personalizada y sofisticada.

            FECHA ACTUAL: $currentDate
            $baseGarmentsPrompt
            $rulesPrompt
            $instructionPrompt
            $avoidPrompt

            INVENTARIO DETALLADO (Solo se incluyen categorías con prendas disponibles):
            ${inventorySections.joinToString("\n\n")}

            INSTRUCCIONES DE RESPUESTA:
            1. Crea una propuesta elegante. Explica POR QUÉ estas prendas combinan bien (color, textura, ocasión).
            2. Evita tecnicismos de base de datos. Sé natural y persuasivo.
            3. NO utilices formato Markdown (sin asteriscos).
            4. OBLIGATORIO: Finaliza con esta línea exacta:
               SELECCION_IDS: SHIRT=id, PANT=id, SHOE=id, TIE=id, WATCH=id, FRAGRANCE=id, JACKET=id, BAG=id, DRESS=id, SKIRT=id
               (Usa "null" si la prenda no es necesaria).
        """.trimIndent()

        return try {
            val response = generativeModel.generateContent(prompt)
            response.text ?: "No se pudo obtener una recomendación en este momento."
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: ""
            if (msg.contains("503") || msg.contains("UNAVAILABLE")) {
                "Error: El servidor de IA está muy ocupado en este momento. Por favor, intenta de nuevo en unos segundos."
            } else if (msg.contains("429") || msg.contains("quota")) {
                "Error: Has alcanzado el límite de peticiones gratuitas. Por favor, espera un minuto antes de generar otra opción."
            } else {
                "Error al consultar a la IA: ${e.localizedMessage}"
            }
        }
    }

    suspend fun analyzeGarmentImage(
        bitmap: Bitmap, 
        categoryName: String? = null,
        customAttributes: String? = null,
        userApiKey: String? = null
    ): String? {
        val categoryContext = if (categoryName != null) {
            "La prenda es un(a): $categoryName."
        } else ""

        val attributesContext = if (!customAttributes.isNullOrBlank()) {
            "Debes detectar estos campos personalizados: $customAttributes."
        } else {
            """
            Detecta con alta precisión:
            - subType: (Tipo específico)
            - brand: (Marca o Sin Marca)
            - color: (Tonalidad exacta: ej. Tan, Oxford, Coñac, Olivo)
            - secondaryColor: (Si aplica)
            - material: (Lana, Seda, Lino, Denim, etc.)
            - pattern: (Liso, Rayas, Cuadros Vichy, Pata de Gallo, etc.)
            - formalityLevel: (Formal, Casual, etc.)
            - other: (Detalles como tipo de cuello, manga, tiro o tacón)
            """.trimIndent()
        }

        val prompt = """
            Actúa como un analista de moda profesional. Analiza la imagen adjunta.
            $categoryContext
            $attributesContext
            
            Reglas de respuesta:
            1. Formato JSON puro y válido.
            2. Sin bloques de código markdown ni texto adicional.
            3. Si no estás seguro de un campo, intenta dar la mejor estimación basada en la visual.
        """.trimIndent()

        val generativeModel = getModel(userApiKey)
        return try {
            val inputContent = content {
                image(bitmap)
                text(prompt)
            }
            val response = generativeModel.generateContent(inputContent)
            response.text
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun generateMannequinImage(
        outfitDescription: String,
        userApiKey: String? = null
    ): Bitmap? {
        // Esta función queda en pausa hasta tener soporte nativo de imagen en Gemini (Plan de pago)
        // o hasta que un modelo fotorrealista compatible sea accesible por API gratuita.
        return null 
    }
}
