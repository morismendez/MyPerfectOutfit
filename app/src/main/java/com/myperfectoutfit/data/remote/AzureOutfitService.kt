package com.myperfectoutfit.data.remote

import android.graphics.Bitmap
import android.util.Base64
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.myperfectoutfit.data.local.entities.*
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
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

        // 1. RECOPILEMOS EL INVENTARIO DETALLADO (Mantenemos tu lógica pulida al 100%)
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
        if (jackets.isNotEmpty()) inventorySections.add("--- CHAQUETAS ---\n" + jackets.joinToString("\n") { "- ID:${it.id} | ${it.brand ?: "S/M"} | ${it.color} | Tipo: ${it.type} | Cierre: ${it.closureType}" })
        if (bags.isNotEmpty()) inventorySections.add("--- BOLSOS ---\n" + bags.joinToString("\n") { "- ID:${it.id} | ${it.brand ?: "S/M"} | ${it.color} | Estilo: ${it.style} | Tamaño: ${it.size}" })
        if (ties.isNotEmpty()) inventorySections.add("--- CORBATAS ---\n" + ties.joinToString("\n") { "- ID:${it.id} | Color: ${it.colorRange} | Diseño: ${it.pattern} | Material: ${it.material}" })
        if (watches.isNotEmpty()) inventorySections.add("--- RELOJES ---\n" + watches.joinToString("\n") { "- ID:${it.id} | ${it.brand} ${it.model} | Color: ${it.dialColor} | Correa: ${it.strapColor} (${it.strapMaterial})" })
        if (fragrances.isNotEmpty()) inventorySections.add("--- FRAGANCIAS ---\n" + fragrances.joinToString("\n") { "- ID:${it.id} | ${it.brand} ${it.name} | Perfil: ${it.profile} | Ocasión: ${it.occasionTag}" })
        if (dresses.isNotEmpty()) inventorySections.add("--- VESTIDOS ---\n" + dresses.joinToString("\n") { "- ID:${it.id} | ${it.brand ?: "S/M"} | ${it.color} | Patrón: ${it.pattern} | Largo: ${it.length} | Manga: ${it.sleeveStyle} | Material: ${it.material}" })
        if (skirts.isNotEmpty()) inventorySections.add("--- FALDAS ---\n" + skirts.joinToString("\n") { "- ID:${it.id} | ${it.brand ?: "S/M"} | ${it.color} | Estilo: ${it.style} | Largo: ${it.length} | Material: ${it.material}" })
        if (customGarments.isNotEmpty()) inventorySections.add("--- OTROS ---\n" + customGarments.joinToString("\n") { "- ID:${it.id} | ${it.attributeValues.replace("|", " - ")}" })

        val basePrompt = if (baseGarments.isNotEmpty()) {
            "\nDEBES INCLUIR OBLIGATORIAMENTE ESTOS IDs:\n" + baseGarments.joinToString("\n") { item ->
                when (item) {
                    is ShirtEntity -> "- Camisa ID:${item.id}"
                    is PantEntity -> "- Pantalón ID:${item.id}"
                    is ShoeEntity -> "- Calzado ID:${item.id}"
                    else -> "- ID de prenda seleccionada"
                }
            }
        } else ""

        val systemInstruction = """
            Eres un Asesor de Imagen y Estilista Personal experto. Crea outfits impecables.
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
        
        val detectPrompt = if (customAttributes != null) "Detecta estos campos personalizados: $customAttributes." else "Detecta: subType (tipo de prenda), brand (marca), color (tonalidad exacta), material (tela), pattern (estampado)."

        val systemInstruction = """
            Eres un analista de moda experto en visión artificial.
            Tu tarea es identificar con precisión milimétrica los detalles de una prenda.
            REGLAS CRÍTICAS:
            1. PRIORIDAD: Busca logotipos, marcas de agua o etiquetas cosidas para identificar la marca (brand).
            2. Si ves texto en la prenda, léelo. Es la clave para la marca.
            3. Si no hay rastro absoluto de marca, usa 'Sin Marca'.
            4. Analiza patrones de estampados, tejidos
            5. Analiza la tela y determina su tipo, sino puedes, la más probable
            6. Responde en JSON puro y en idioma $languageName.
        """.trimIndent()

        val userPromptText = "Analiza detalladamente esta foto de ${categoryName ?: "ropa"}. $detectPrompt. Sé extremadamente observador con la marca y el material."

        val data = mapOf(
            "systemInstruction" to systemInstruction,
            "userPrompt" to userPromptText,
            "image" to "data:image/jpeg;base64,$base64Image"
        )

        return try {
            val result = functions
                .getHttpsCallable("analyzeGarment")
                .call(data)
                .await()
            
            result.data as String
        } catch (e: Exception) {
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
