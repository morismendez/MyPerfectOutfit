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

import com.myperfectoutfit.domain.model.GarmentItem

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
        baseGarments: List<GarmentItem> = emptyList(),
        userInstruction: String = "",
        previousOutfits: List<String> = emptyList()
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
            "\nRESTRICCIÓN OBLIGATORIA DE PRENDAS BASE: El usuario exige usar estas prendas en la combinación. DEBES incluir EXACTAMENTE estos IDs de prenda:\n" + baseGarments.joinToString("\n") { item ->
                "- DEBES INCLUIR OBLIGATORIAMENTE ID: ${item.categoryTag}=${item.id} (${item.formattedDescription})"
            }
        } else ""

        val userInstructionFormatted = if (userInstruction.isNotBlank()) {
            "\nINSTRUCCIÓN Y PREFERENCIA PRIORITARIA DEL USUARIO: \"$userInstruction\"\n" +
            "REGLA DE CUMPLIMIENTO ESTRICTO: La instrucción del usuario es OBLIGATORIA. Si el usuario indica que hace calor, o que no desea usar un tipo de prenda específico (ej. chaqueta, abrigo, suéter, corbata), DEBES omitir esa prenda por completo dejando su tag como NONE (ejemplo: JACKET=NONE, TIE=NONE)."
        } else ""

        val excludePrompt = if (previousOutfits.isNotEmpty()) {
            "\nRESTRICCIÓN DE NO REPETICIÓN DE OUTFITS:\n" +
                    "Ya le has mostrado al usuario las siguientes combinaciones exactas en intentos anteriores:\n" +
                    previousOutfits.joinToString("\n") { "- $it" } + "\n" +
                    "DEBES generar una alternativa NUEVA y DIFERENTE. No repitas exactamente la misma combinación de prendas juntas. (Nota: puedes usar las mismas prendas individualmente si las combinas de una manera completamente distinta a las listas anteriores)."
        } else ""
        val systemInstruction = """
            Eres un Asesor de Imagen y Estilista Personal experto de lujo. Tu objetivo es crear outfits impecables y brindar asesoría sofisticada.
            
            ESTRUCTURA DE RESPUESTA OBLIGATORIA:
            1. PRIMERO LA NARRATIVA: Escribe siempre 2 párrafos detallados, pulidos y elegantes en los que expliques la armonía de colores de la propuesta, la ocasión ideal y consejos de estilo prácticos. NO coloques la línea SELECCION_IDS en los párrafos ni al principio.
            2. ÚLTIMA LÍNEA DE SELECCIÓN: Al final de todo tu mensaje, en una línea nueva e independiente, incluye el tag exacto de IDs numéricos:
               SELECCION_IDS: SHIRT=id, PANT=id, SHOE=id, TIE=id, WATCH=id, FRAGRANCE=id, JACKET=id, BAG=id, DRESS=id, SKIRT=id, CUSTOM=id

            REGLAS DE ORO Y SEGURIDAD:
            1. OUTFIT COMPLETO OBLIGATORIO: Todo outfit sugerido DEBE incluir prendas superiores, inferiores y calzado (o vestido + calzado), más accesorios o fragancia acordes.
            2. PRENDAS BASE: Si el usuario seleccionó prendas base, inclúyelas obligatoriamente en tu combinación y explica cómo combinarlas con el resto del inventario.
            3. DIVERSIFICACIÓN: Si ya se han mostrado combinaciones anteriores, cambia los pantalones, calzado y accesorios para ofrecer alternativas distintas.
            4. IDIOMA Y ESTILO: Responde SIEMPRE en $languageName sofisticado, elegante y natural. Sin Markdown (sin asteriscos o numerales).
        """.trimIndent()

        val userPrompt = """
            HOY ES: $currentDate
            ${if (styleRules.isNotEmpty()) "REGLAS: " + styleRules.joinToString { it.description } else ""}
            $userInstructionFormatted
            $basePrompt
            $excludePrompt

            INVENTARIO DE PRENDAS DISPONIBLES:
            ${inventorySections.joinToString("\n\n")}

            FINALIZA OBLIGATORIAMENTE EN LA ÚLTIMA LÍNEA CON: SELECCION_IDS: SHIRT=id, PANT=id, SHOE=id, TIE=id, WATCH=id, FRAGRANCE=id, JACKET=id, BAG=id, DRESS=id, SKIRT=id, CUSTOM=id
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
            1. IGNORA EL FONDO: Solo analiza la prenda o articulo principal.
            2. OBJETIVIDAD: Si un atributo no es visible con claridad en la imagen, asigna un valor vacío (""). NO inventes ni asumas datos no visibles
            3. FORMATO: Devuelve SOLO un objeto JSON puro. Sin explicaciones.
            4. IDIOMA: Responde en $languageName.
        """.trimIndent()

        val userPromptText = "Analiza detenidamente la imagen adjunta y rellena los campos: $fields."

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
                "Sastre y Estilista Experto",
                "1. MARCA: Inspecciona el interior del cuello, estampado o etiquetas visibles. " +
                        "2. COLOR: Distingue el 'primaryColor' dominante y el 'secondaryColor' (si aplica). " +
                        "3. TIPO Y MANGA: Clasifica 'subType' (Camisa, Polo, Camiseta, Blusa, Top) y 'sleeveLength' (Manga larga, Manga corta, Sin mangas, 3/4). " +
                        "4. CUELLO Y MATERIAL:Identifica 'necklineStyle' (Camisero, Cuello redondo, Cuello V, Polo, Mao) y 'material' (Algodón, Lino, Seda, Sintético)."
            )
            normalized.contains("reloj") -> ExpertContext("Relojero Experto", "1. MARCA Y MODELO: Lee con precisión la marca y modelo impresos en la esfera o bisel. 2. MATERIAL Y COLOR: Describe el material de la caja y el color/material de la correa ('strapColor', 'strapMaterial').")
            normalized.contains("fragancia") || normalized.contains("perfume") -> ExpertContext(
                "Especialista en Perfumería",
                "1. FRASCO Y EMPAQUE: Identifica la marca ('brand') y el nombre exacto de la fragancia ('name') impresos en el frasco o caja. 2. PERFIL Y OCASIÓN: Estima el 'profile' olfativo (Fresco, Amaderado, Cítrico, Dulce, Oriental) y la 'occasionTag' (Diario, Noche, Formal)."
            )
            normalized.contains("pantalón") || normalized.contains("pantalon") || normalized.contains("inferior") -> ExpertContext(
                "Estilista de Moda",
                "1. TIPO: Determina si es 'subType' (Jeans, Chinos, Cargo, etc.). 2. CORTE: Identifica 'fitStyle' (Slim, Recto, etc.). 3. CINTURA: Determina 'waistRise' (Bajo, Medio, Alto, etc). 4. MARCA: Busca etiquetas, logos, parches o grabados."
            )
            normalized.contains("calzado") || normalized.contains("zapato") || normalized.contains("tenis") -> ExpertContext(
                "Experto en Calzado",
                "1. TIPO Y ESTILO: Determina 'subType' (Tenis, Zapatos de vestir, Botas, Sandalias, etc) y 'style' (Oxford, Derby, Loafer, Sneaker, Chelsea, etc). 2. CIERRE: Nota el 'closureType' (Cordones, Hebilla, slip-on, etc). 3. PUNTERA: Mira 'toeStyle' (Puntera lisa, Cap Toe, Redonda, etc) y 'material' (Cuero, Gamuza, Tela, etc)."
            )
            normalized.contains("chaqueta") || normalized.contains("abrigo") -> ExpertContext(
                "Especialista en Prendas de Exterior",
                "1. TIPO: Clasifica en 'type' (Blazer, Bomber, Parka, etc). 2. CIERRE: Identifica 'closureType' (Botones, Cremallera, etc). 3. COLOR: Enfócate solo en la prenda principal."
            )
            normalized.contains("corbata") -> ExpertContext(
                "Consultor de Imagen",
                "1. DISEÑO: Identifica 'pattern' (Liso, Lunares, Rayas, etc). 2. COLOR: Define 'colorRange'. 3. ANCHO: Estima 'widthCms' (ej: 7.5)."
            )
            normalized.contains("bolso") || normalized.contains("cartera") -> ExpertContext(
                "Experto en Marroquinería",
                "1. ESTILO: Determina 'style' (Tote, Clutch, Mochila, etc). 2. TAMAÑO: Estima 'size' (Pequeño, Mediano, Grande, etc)."
            )
            normalized.contains("vestido") || normalized.contains("falda") -> ExpertContext(
                "Estilista Femenino",
                "1. CORTE: Determina 'style' (Plisada, Lápiz, A-line, etc) y 'length' (Mini, Midi, Maxi, etc). 2. MANGA: Si es vestido, identifica 'sleeveStyle'."
            )
            else -> ExpertContext(
                "Especialista en ${category ?: "Moda"}",
                "Analiza minuciosamente este artículo de ${category ?: "tu inventario"} y extrae los siguientes datos con precisión técnica: $fields."
            )
        }
    }

    private fun cleanBrand(brand: String?): String {
        if (brand.isNullOrBlank() || brand.lowercase() == "sin marca" || brand.lowercase() == "null") return ""
        return brand
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
        val maxDim = 1280
        val (newW, newH) = if (bitmap.width > bitmap.height) {
            maxDim to (maxDim * bitmap.height / bitmap.width)
        } else {
            (maxDim * bitmap.width / bitmap.height) to maxDim
        }
        val resized = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        val out = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 85, out)
        val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        if (resized != bitmap) {
            resized.recycle()
        }
        return base64
    }
}
