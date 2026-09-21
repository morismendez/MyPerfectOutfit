package com.myperfectoutfit.data.remote

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.FirebaseFunctionsException
import com.myperfectoutfit.data.local.entities.*
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.lang.reflect.Modifier
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AzureOutfitService @Inject constructor() {

    private val functions = Firebase.functions("us-central1")

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
    ): String {
        val deviceLocale = Locale.getDefault()
        val languageName = deviceLocale.displayLanguage
        val currentDate = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", deviceLocale).format(Date())

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
        val basePrompt = if (baseGarments.isNotEmpty()) {
            "\nDEBES INCLUIR OBLIGATORIAMENTE ESTOS IDs:\n" + baseGarments.joinToString("\n") { item ->
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
            }
        } else ""

        val systemInstruction = """
            Eres un Asesor de Imagen y Estilista Personal experto. Crea outfits impecables.
            ${if (excludeIds.isNotEmpty()) "IMPORTANTE: Solo puedes usar las prendas listadas en el inventario. EVITA usar estas prendas: " + excludeIds.joinToString() else ""}
            REGLAS DE ORO:
            1. ARMONÍA: Aplica teoría del color.
            2. OCASIÓN: Selecciona calzado acorde a la formalidad.
            3. IDIOMA: Responde SIEMPRE en $languageName elegante y natural.
            4. FORMATO: NO uses Markdown (sin asteriscos).
            5. PROHIBIDO: No menciones códigos ID en la descripción narrativa. Los IDs solo van en la línea final.
        """.trimIndent()

        val userPrompt = """
            HOY ES: $currentDate
            ${if (styleRules.isNotEmpty()) "REGLAS: " + styleRules.joinToString { it.description } else ""}
            ${if (excludeIds.isNotEmpty()) "EVITA ESTOS IDs: " + excludeIds.joinToString() else ""}
            $basePrompt
            INSTRUCCIÓN: $userInstruction

            INVENTARIO:
            ${inventorySections.joinToString("\n\n")}

            FINALIZA CON: SELECCION_IDS: SHIRT=id, PANT=id, SHOE=id, TIE=id, WATCH=id, FRAGRANCE=id, JACKET=id, BAG=id, DRESS=id, SKIRT=id
        """.trimIndent()

        val data = mapOf(
            "systemInstruction" to systemInstruction,
            "userPrompt" to userPrompt
        )

        return try {
            val result = functions
                .getHttpsCallable("getOutfitRecommendation")
                .call(data)
                .await()
            
            result.data as String
        } catch (e: Exception) {
            "Error en el servidor: ${e.localizedMessage}"
        }
    }

    suspend fun analyzeGarmentImage(
        bitmap: Bitmap, 
        categoryName: String? = null,
        customAttributes: String? = null
    ): String? {
        val base64Image = bitmapToBase64(bitmap)
        val deviceLocale = Locale.getDefault()
        val languageName = deviceLocale.displayLanguage
        
        val fields = getFieldsForCategory(categoryName) ?: customAttributes ?: "subType, brand, color, material, pattern"
        val expertContext = getExpertInstructions(categoryName, fields)

        val systemInstruction = """
            Eres un ${expertContext.expertType}.
            ${expertContext.instructions}
            
            REGLAS:
            1. IGNORA EL FONDO: Solo analiza la prenda principal.
            2. FORMATO: Devuelve SOLO un objeto JSON puro. Sin explicaciones.
            3. IDIOMA: Responde en $languageName.
        """.trimIndent()

        val userPromptText = "Inspecciona este artículo y rellena los campos: $fields."

        val data = mapOf(
            "systemInstruction" to systemInstruction,
            "userPrompt" to userPromptText,
            "image" to "data:image/jpeg;base64,$base64Image"
        )

        return try {
            Log.d("AzureOutfitService", "Enviando imagen a analyzeGarment...")
            val result = functions
                .getHttpsCallable("analyzeGarment")
                .call(data)
                .await()
            
            val response = result.data as String
            val cleaned = response.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            
            if (cleaned.isBlank()) "Error: Respuesta de IA vacía" else cleaned

        } catch (e: FirebaseFunctionsException) {
            val errorMsg = "Functions Error: ${e.code} - ${e.message}"
            Log.e("AzureOutfitService", errorMsg, e)
            errorMsg
        } catch (e: Exception) {
            val errorMsg = "General Error: ${e.localizedMessage ?: e.message}"
            Log.e("AzureOutfitService", errorMsg, e)
            errorMsg
        }
    }

    private data class ExpertContext(val expertType: String, val instructions: String)

    private fun getExpertInstructions(category: String?, fields: String): ExpertContext {
        val normalized = category?.lowercase()?.trim() ?: ""
        
        return when {
            normalized.contains("camisa") || normalized.contains("top") -> ExpertContext(
                "Sastre Experto",
                "Mira el interior del cuello para la marca (Covington es muy probable). El color es Amarillo/Crema, ignora el ladrillo."
            )
            normalized.contains("reloj") -> ExpertContext("Relojero", "Lee marca y modelo en la esfera.")
            normalized.contains("fragancia") || normalized.contains("perfume") -> ExpertContext(
                "Especialista en Perfumería",
                "1. FRASCO: Identifica 'brand' y 'name' (ej: Hugo Red). 2. SÉ BREVE: Sin descripciones del envase. 3. FORMATO: Sin listas, solo texto separado por comas."
            )
            normalized.contains("pantalón") || normalized.contains("pantalon") || normalized.contains("inferior") -> ExpertContext(
                "Estilista de Moda",
                "1. TIPO: Determina si es 'subType' (Jeans, Chinos, Cargo, etc.). 2. CORTE: Identifica 'fitStyle' (Slim, Recto, etc.). 3. CINTURA: Determina 'waistRise' (Bajo, Medio, Alto). 4. MARCA: Busca etiquetas en la cintura o bolsillo trasero."
            )
            normalized.contains("calzado") || normalized.contains("zapato") || normalized.contains("tenis") -> ExpertContext(
                "Experto en Calzado",
                "1. ESTILO: Identifica 'subType' (Tenis, Oxford, Bota) y 'style' (Loafer, Derby). 2. CIERRE: Nota el 'closureType' (Cordones, Hebilla). 3. PUNTERA: Mira 'toeStyle' (Redonda, Cuadrada)."
            )
            normalized.contains("chaqueta") || normalized.contains("abrigo") -> ExpertContext(
                "Especialista en Prendas de Exterior",
                "1. TIPO: Clasifica en 'type' (Blazer, Bomber, Parka). 2. CIERRE: Identifica 'closureType' (Botones, Cremallera). 3. COLOR: Enfócate solo en la prenda principal."
            )
            normalized.contains("corbata") -> ExpertContext(
                "Consultor de Imagen",
                "1. DISEÑO: Identifica 'pattern' (Liso, Lunares, Rayas). 2. COLOR: Define 'colorRange'. 3. ANCHO: Estima 'widthCms' (ej: 7.5)."
            )
            normalized.contains("bolso") || normalized.contains("cartera") -> ExpertContext(
                "Experto en Marroquinería",
                "1. ESTILO: Determina 'style' (Tote, Clutch, Mochila). 2. TAMAÑO: Estima 'size' (Pequeño, Mediano, Grande)."
            )
            normalized.contains("vestido") || normalized.contains("falda") -> ExpertContext(
                "Estilista Femenino",
                "1. CORTE: Determina 'style' (Plisada, Lápiz) y 'length' (Mini, Midi, Maxi). 2. MANGA: Si es vestido, identifica 'sleeveStyle'."
            )
            else -> ExpertContext(
                "Especialista en ${category ?: "Moda"}",
                "Analiza minuciosamente este artículo de ${category ?: "tu inventario"} y extrae los siguientes datos con precisión técnica: $fields."
            )
        }
    }

    private fun getFieldsForCategory(categoryName: String?): String? {
        val normalized = categoryName?.lowercase()?.trim() ?: return null
        val clazz = when {
            normalized.contains("camisa") || normalized.contains("top") -> ShirtEntity::class.java
            normalized.contains("pantalón") || normalized.contains("pantalon") || normalized.contains("inferior") -> PantEntity::class.java
            normalized.contains("calzado") || normalized.contains("zapato") || normalized.contains("tenis") -> ShoeEntity::class.java
            normalized.contains("chaqueta") || normalized.contains("abrigo") || normalized.contains("suéter") || normalized.contains("sueter") -> JacketEntity::class.java
            normalized.contains("reloj") -> WatchEntity::class.java
            normalized.contains("bolso") || normalized.contains("cartera") -> BagEntity::class.java
            normalized.contains("corbata") -> TieEntity::class.java
            normalized.contains("fragancia") || normalized.contains("perfume") -> FragranceEntity::class.java
            normalized.contains("vestido") -> DressEntity::class.java
            normalized.contains("falda") -> SkirtEntity::class.java
            else -> return null
        }
        return getFieldsForClass(clazz)
    }

    private fun getFieldsForClass(clazz: Class<*>): String {
        val excluded = setOf("id", "userId", "code", "imageUrl", "laundryState", "isAvailable")
        return clazz.declaredFields
            .filter { field ->
                !field.isSynthetic && 
                !Modifier.isStatic(field.modifiers) &&
                !excluded.contains(field.name)
            }
            .joinToString(", ") { it.name }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDim = 800
        val (newW, newH) = if (bitmap.width > bitmap.height) {
            maxDim to (maxDim * bitmap.height / bitmap.width)
        } else {
            (maxDim * bitmap.width / bitmap.height) to maxDim
        }
        val resized = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        val out = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 70, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
