package com.myperfectoutfit.data.remote

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.myperfectoutfit.BuildConfig
import com.myperfectoutfit.data.local.entities.*
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
        styleRules: List<StyleRuleEntity> = emptyList(),
        baseGarments: List<Any> = emptyList(),
        userInstruction: String = "",
        excludeIds: List<String> = emptyList(),
        userApiKey: String? = null
    ): String {
        val currentDate = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES")).format(Date())
        val generativeModel = getModel(userApiKey)

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
            Eres un asesor de imagen y estilista personal experto.
            Tu objetivo es analizar mi inventario disponible y recomendar la mejor combinación para hoy.

            FECHA ACTUAL: $currentDate
            $baseGarmentsPrompt
            $rulesPrompt
            $instructionPrompt
            $avoidPrompt

            INVENTARIO LIMPIO Y DISPONIBLE HOY:

            --- CAMISAS/TOPS DISPONIBLES ---
            ${shirts.joinToString("\n") { "- ID:${it.id} | Tipo: ${it.subType} | Marca: ${it.brand} | Color: ${it.primaryColor} (Secundario: ${it.secondaryColor ?: "N/A"}) | Patrón: ${it.pattern} | Manga: ${it.sleeveLength} | Cuello/Escote: ${it.necklineStyle} | Material: ${it.material} | Formalidad: ${it.formalityLevel}" }}

            --- PANTALONES/INFERIORES DISPONIBLES ---
            ${pants.joinToString("\n") { "- ID:${it.id} | Tipo: ${it.subType} | Marca: ${it.brand ?: "S/M"} | Color: ${it.primaryColor} (Secundario: ${it.secondaryColor ?: "N/A"}) | Material: ${it.material} | Largo: ${it.lengthStyle} | Tiro: ${it.waistRise} | Corte: ${it.fitStyle} | Formalidad: ${it.formalityLevel}" }}

            --- CALZADO DISPONIBLE ---
            ${shoes.joinToString("\n") { "- ID:${it.id} | Tipo: ${it.subType} | Estilo: ${it.style} | Marca: ${it.brand} | Color: ${it.color} (Secundario: ${it.secondaryColor ?: "N/A"}) | Material: ${it.material} | Tacón: ${it.heelHeightStyle} | Puntera: ${it.toeStyle} | Cierre: ${it.closureType} | Formalidad: ${it.formalityLevel}" }}

            --- CORBATAS DISPONIBLES ---
            ${ties.joinToString("\n") { "- ID:${it.id} | Gama: ${it.colorRange} | Diseño: ${it.pattern}" }}

            --- RELOJES DISPONIBLES ---
            ${watches.joinToString("\n") { "- ID:${it.id} | Marca: ${it.brand} | Modelo: ${it.model} | Correa: ${it.strapColor} (${it.strapMaterial})" }}

            --- FRAGANCIAS DISPONIBLES ---
            ${fragrances.joinToString("\n") { "- ID:${it.id} | Marca: ${it.brand} | Nombre: ${it.name}" }}
            
            --- CHAQUETAS DISPONIBLES ---
            ${jackets.joinToString("\n") { "- ID:${it.id} | Marca: ${it.brand ?: "S/M"} | Color: ${it.color} | Tipo: ${it.type}" }}

            --- BOLSOS/CARTERAS DISPONIBLES ---
            ${bags.joinToString("\n") { "- ID:${it.id} | Marca: ${it.brand ?: "S/M"} | Estilo: ${it.style} | Color: ${it.color} | Tamaño: ${it.size}" }}

            --- VESTIDOS DISPONIBLES ---
            ${dresses.joinToString("\n") { "- ID:${it.id} | Marca: ${it.brand ?: "S/M"} | Color: ${it.color} | Estampado: ${it.pattern} | Largo: ${it.length}" }}

            --- FALDAS DISPONIBLES ---
            ${skirts.joinToString("\n") { "- ID:${it.id} | Marca: ${it.brand ?: "S/M"} | Color: ${it.color} | Estilo: ${it.style} | Largo: ${it.length}" }}

            INSTRUCCIONES DE RESPUESTA:
            1. Selecciona exactamente las prendas que arman el conjunto (Camisa+Pantalón/Falda O Vestido, Calzado, y si aplica, Bolso, Corbata, Reloj, Fragancia y Chaqueta).
            2. Tu explicación debe ser natural y elegante. NO utilices asteriscos (**) para negritas ni otros símbolos de formato Markdown. 
            3. NO menciones IDs de prendas ni nombres de campos técnicos (como "laundryState").
            4. Si una categoría no es necesaria (ej. no hay corbata que combine), simplemente NO la menciones; no digas "No aplica" ni "null".
            5. Incluye la instrucción del usuario en tu razonamiento de forma integrada.
            6. CRÍTICO: Al final de tu respuesta, añade una ÚNICA LÍNEA con el formato exacto: 
               SELECCION_IDS: SHIRT=id, PANT=id, SHOE=id, TIE=id, WATCH=id, FRAGRANCE=id, JACKET=id, BAG=id, DRESS=id, SKIRT=id
               (Si alguna categoría no aplica, pon "null" en lugar del id, ej: TIE=null).
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
            "La prenda pertenece a la categoría: $categoryName."
        } else ""

        val attributesContext = if (!customAttributes.isNullOrBlank()) {
            "Debes detectar obligatoriamente estos campos específicos: $customAttributes."
        } else {
            """
            - subType: (Tipo específico de la categoría)
            - brand: (Marca detectada o "Sin Marca")
            - color: (Tonalidad exacta, ej: "Café Coñac", "Gris Oxford")
            - secondaryColor: (Si aplica)
            - material: (Lana, Denim, Cuero, etc.)
            - pattern: (Liso, Rayas, Cuadros, etc.)
            - formalityLevel: (Formal, Casual, etc.)
            - other: (Detalles como cuello, manga, tiro o tacón según aplique)
            """.trimIndent()
        }

        val prompt = """
            Analiza esta imagen de una prenda de vestir con ojo de estilista experto.
            $categoryContext
            $attributesContext
            
            Devuelve la información en formato JSON puro (sin bloques de código ```json).
            Sé conciso y preciso para minimizar el tiempo de respuesta.
            Si no puedes identificar la prenda, devuelve un JSON con un campo "error".
        """.trimIndent()

        val generativeModel = getModel(userApiKey)
        return try {
            val inputContent = content {
                image(bitmap)
                text(prompt)
            }
            val response = generativeModel.generateContent(inputContent)
            response.text
        } catch (e: Exception) {
            null
        }
    }
}
