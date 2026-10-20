package com.myperfectoutfit.data.remote

import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.myperfectoutfit.data.local.entities.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AzureSuitcaseService @Inject constructor() {

    private val functions = Firebase.functions("us-central1")

    suspend fun generateSuitcaseRecommendation(
        tripName: String,
        days: Int,
        suitcaseType: String,
        tripStyle: String,
        specialEvents: String,
        inventory: Map<String, List<Any>>,
        userLanguage: String
    ): String {
        val systemInstruction = """
            Eres un experto en organización de viajes y estilista personal. Tu tarea es generar una lista de empaque optimizada.
            REGLAS DE EMPAQUE:
            1. REUTILIZACIÓN: Prioriza prendas inferiores que combinen con múltiples superiores.
            2. RESTRICCIÓN: Si es Carry-on, sé muy agresivo limitando zapatos y abrigos.
            3. FORMULA: Sugiere aprox. 1 prenda superior por día + 1 repuesto. Para inferiores, permite mayor variedad si el inventario lo permite (ej. 1 cada 2 días o incluso más si hay opciones distintas que combinen bien), no limites a una sola prenda inferior si el viaje dura más de 2 días.
            4. VARIEDAD: Si el usuario tiene múltiples opciones de pantalones/faldas, intenta incluir al menos 2 estilos diferentes para evitar la monotonía, a menos que sea una maleta de mano muy pequeña.
            5. FALTANTES: Si el viaje requiere prendas que NO están en el inventario (ej. sandalias para playa, abrigo para frío), DEBES sugerirlas en la sección "missingSuggestions".
            6. ESENCIALES: Sugiere siempre artículos básicos (ropa interior, calcetines, pijama, kit de aseo) calculados para los $days días en la sección "essentials".
            7. IDIOMA: Responde en $userLanguage.
            8. FORMATO: Devuelve ÚNICAMENTE un JSON con esta estructura:
               {
                 "justification": "Breve explicación del estilo elegido",
                 "selectedGarmentIds": ["ID1", "ID2"],
                 "missingSuggestions": ["Sugerencia 1", "Sugerencia 2"],
                 "essentials": ["Articulo 1", "Articulo 2"]
               }
        """.trimIndent()

        val inventoryDescription = inventory.map { (cat, items) ->
            "$cat: ${items.joinToString(", ") { item ->
                when(item) {
                    is ShirtEntity -> "ID:SHIRT_${item.id}(${item.brand} ${item.primaryColor})"
                    is PantEntity -> "ID:PANT_${item.id}(${item.brand ?: ""} ${item.primaryColor})"
                    is ShoeEntity -> "ID:SHOE_${item.id}(${item.brand} ${item.color})"
                    is JacketEntity -> "ID:JACKET_${item.id}(${item.brand ?: ""} ${item.color})"
                    is DressEntity -> "ID:DRESS_${item.id}(${item.brand ?: ""} ${item.color})"
                    is SkirtEntity -> "ID:SKIRT_${item.id}(${item.brand ?: ""} ${item.color})"
                    is TieEntity -> "ID:TIE_${item.id}(${item.colorRange} ${item.pattern})"
                    is BagEntity -> "ID:BAG_${item.id}(${item.brand ?: ""} ${item.color})"
                    is WatchEntity -> "ID:WATCH_${item.id}(${item.brand} ${item.model})"
                    is CustomGarmentEntity -> "ID:CUSTOM_${item.id}(CatID:${item.categoryId}: ${item.attributeValues})"
                    else -> "ID:UNKNOWN"
                }
            }}"
        }.joinToString("\n")

        val userPrompt = """
            VIAJE: $tripName
            DURACIÓN: $days días
            TIPO MALETA: $suitcaseType
            ESTILO: $tripStyle
            EVENTOS: $specialEvents

            INVENTARIO DISPONIBLE:
            $inventoryDescription

            Selecciona las mejores prendas para este viaje.
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
            "Error en la Maleta IA: ${e.localizedMessage}"
        }
    }

    suspend fun getReplacementRecommendation(
        tripContext: String,
        currentSuitcase: String,
        garmentToReplace: String,
        alternatives: String,
        userLanguage: String
    ): String {
        val systemInstruction = """
            Eres un experto en moda y estilismo. Tu tarea es ayudar al usuario a reemplazar una prenda de su maleta.
            Debes elegir la mejor alternativa que combine con el resto de las prendas ya seleccionadas y sea adecuada para el viaje.
            IDIOMA: Responde en $userLanguage.
            FORMATO: Devuelve ÚNICAMENTE un JSON con esta estructura:
            {
              "selectedId": "ID_DE_LA_PRENDA_ELEGIDA",
              "reason": "Breve explicación de por qué esta es la mejor opción"
            }
        """.trimIndent()

        val userPrompt = """
            CONTEXTO DEL VIAJE: $tripContext
            MALETA ACTUAL: $currentSuitcase
            PRENDA A CAMBIAR: $garmentToReplace
            ALTERNATIVAS DISPONIBLES:
            $alternatives

            Elige la mejor alternativa de la lista para reemplazar la prenda.
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
            "Error en el Cambio IA: ${e.localizedMessage}"
        }
    }
}