package com.myperfectoutfit.data.remote

import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.myperfectoutfit.data.local.entities.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AzurePlanningService @Inject constructor() {

    private val functions = Firebase.functions("us-central1")

    suspend fun generateMultiDayPlanning(
        daysCount: Int,
        startDate: Date,
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
        userInstruction: String = ""
    ): String {
        val deviceLocale = Locale.getDefault()
        val languageName = deviceLocale.displayLanguage
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val startDateStr = sdf.format(startDate)

        // 1. RECOPILEMOS EL INVENTARIO (Simplificado para el prompt de planificación masiva)
        val inventory = StringBuilder()
        if (shirts.isNotEmpty()) inventory.append("CAMISAS: ${shirts.joinToString(", ") { "ID:${it.id}(${it.brand} ${it.primaryColor} ${it.subType})" }}\n")
        if (pants.isNotEmpty()) inventory.append("PANTALONES: ${pants.joinToString(", ") { "ID:${it.id}(${it.brand ?: ""} ${it.primaryColor} ${it.subType})" }}\n")
        if (shoes.isNotEmpty()) inventory.append("ZAPATOS: ${shoes.joinToString(", ") { "ID:${it.id}(${it.brand} ${it.color} ${it.style})" }}\n")
        if (jackets.isNotEmpty()) inventory.append("CHAQUETAS: ${jackets.joinToString(", ") { "ID:${it.id}(${it.brand ?: ""} ${it.color} ${it.type})" }}\n")
        if (ties.isNotEmpty()) inventory.append("CORBATAS: ${ties.joinToString(", ") { "ID:${it.id}(${it.colorRange} ${it.pattern})" }}\n")
        if (watches.isNotEmpty()) inventory.append("RELOJES: ${watches.joinToString(", ") { "ID:${it.id}(${it.brand} ${it.model})" }}\n")
        if (fragrances.isNotEmpty()) inventory.append("FRAGANCIAS: ${fragrances.joinToString(", ") { "ID:${it.id}(${it.brand} ${it.name})" }}\n")
        if (bags.isNotEmpty()) inventory.append("BOLSOS: ${bags.joinToString(", ") { "ID:${it.id}(${it.brand ?: ""} ${it.color} ${it.style})" }}\n")
        if (dresses.isNotEmpty()) inventory.append("VESTIDOS: ${dresses.joinToString(", ") { "ID:${it.id}(${it.brand ?: ""} ${it.color})" }}\n")
        if (skirts.isNotEmpty()) inventory.append("FALDAS: ${skirts.joinToString(", ") { "ID:${it.id}(${it.brand ?: ""} ${it.color})" }}\n")
        if (customGarments.isNotEmpty()) inventory.append("PERSONALIZADOS: ${customGarments.joinToString(", ") { "ID:${it.id}(${it.attributeValues})" }}\n")

        val rulesStr = if (styleRules.isNotEmpty()) "REGLAS DE ESTILO:\n${styleRules.joinToString("\n") { "- ${it.title}: ${it.description}" }}\n" else ""

        val systemInstruction = """
            Eres un Planificador de Moda experto. Tu tarea es generar una serie de outfits para varios días.
            IMPORTANTE: Solo puedes usar las prendas listadas en el INVENTARIO proporcionado. No inventes prendas.
            REGLAS:
            1. NO REPITAS la misma combinación exacta en el periodo solicitado.
            2. ARMONÍA: Aplica teoría del color y estilo.
            3. IDIOMA: Responde SIEMPRE en $languageName.
            4. FORMATO: Devuelve una lista de outfits, uno por día.
            5. OBLIGATORIO: Por cada día, usa el formato:
               FECHA: yyyy-MM-dd
               NOTAS: Breve explicación del look.
               IDS: SHIRT=id, PANT=id, SHOE=id, TIE=id, WATCH=id, FRAGRANCE=id, JACKET=id, BAG=id, DRESS=id, SKIRT=id
               (Usa "null" si el ID no aplica).
        """.trimIndent()

        val userPrompt = """
            PLANIFICAR PARA: $daysCount días, comenzando el $startDateStr.
            INSTRUCCIÓN ESPECIAL: "$userInstruction"
            
            $rulesStr
            INVENTARIO:
            $inventory

            Genera los outfits respetando la variedad y el estilo personal.
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
            "Error en el servidor de planificación: ${e.localizedMessage}"
        }
    }
}