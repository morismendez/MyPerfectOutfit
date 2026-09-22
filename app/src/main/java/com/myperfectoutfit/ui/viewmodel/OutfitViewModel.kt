package com.myperfectoutfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.data.remote.AzureOutfitService
import com.myperfectoutfit.data.repository.SuitcaseRepository
import com.myperfectoutfit.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecommendedOutfit(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val shirt: ShirtEntity? = null,
    val pant: PantEntity? = null,
    val shoe: ShoeEntity? = null,
    val tie: TieEntity? = null,
    val watch: WatchEntity? = null,
    val fragrance: FragranceEntity? = null,
    val jacket: JacketEntity? = null,
    val bag: BagEntity? = null,
    val dress: DressEntity? = null,
    val skirt: SkirtEntity? = null,
    val customGarments: List<CustomGarmentEntity> = emptyList()
)

data class AiOutfitUiState(
    val isLoading: Boolean = false,
    val isGeneratingMannequin: Boolean = false,
    val mannequinImage: android.graphics.Bitmap? = null,
    val userInstruction: String = "",
    val activeInstruction: String = "", // La instrucción que se usó para las recomendaciones actuales
    val recommendations: List<RecommendedOutfit> = emptyList(),
    val baseGarments: List<Any> = emptyList(),
    val availableGarments: Map<String, List<Any>> = emptyMap(),
    val isOutfitConfirmed: Boolean = false,
    val isTripModeActive: Boolean = false,
    val activeTripName: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class OutfitViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val suitcaseRepository: SuitcaseRepository,
    private val azureService: AzureOutfitService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiOutfitUiState())
    val uiState: StateFlow<AiOutfitUiState> = _uiState.asStateFlow()

    init {
        loadAvailableGarments()
    }

    private fun loadAvailableGarments() {
        viewModelScope.launch {
            repository.getPrimaryUser().firstOrNull()?.let { user ->
                val userId = user.id
                val activeCats = user.activeCategories.split(",")
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).apply {
                    timeZone = java.util.TimeZone.getTimeZone("UTC")
                }.format(java.util.Date())

                combine(
                    suitcaseRepository.getActiveSuitcase(userId, today),
                    if (activeCats.contains("SHIRTS")) repository.getCleanShirts(userId) else flowOf(emptyList()),
                    if (activeCats.contains("PANTS")) repository.getCleanPants(userId) else flowOf(emptyList()),
                    if (activeCats.contains("SHOES")) repository.getAvailableShoes(userId) else flowOf(emptyList()),
                    if (activeCats.contains("TIES")) repository.getCleanTies(userId) else flowOf(emptyList()),
                    if (activeCats.contains("WATCHES")) repository.getAvailableWatches(userId) else flowOf(emptyList()),
                    if (activeCats.contains("FRAGRANCES")) repository.getAvailableFragrances(userId) else flowOf(emptyList()),
                    if (activeCats.contains("JACKETS")) repository.getCleanJackets(userId) else flowOf(emptyList()),
                    if (activeCats.contains("BAGS")) repository.getAvailableBags(userId) else flowOf(emptyList()),
                    if (activeCats.contains("DRESSES")) repository.getCleanDresses(userId) else flowOf(emptyList()),
                    if (activeCats.contains("SKIRTS")) repository.getCleanSkirts(userId) else flowOf(emptyList()),
                    repository.getAllCustomGarments(userId)
                ) { array ->
                    val activeSuitcase = array[0] as? SuitcaseEntity
                    val suitcaseItemIds = activeSuitcase?.garmentIds?.split(",")?.map { it.split(":")[0] }?.toSet() ?: emptySet()

                    fun <T> filterBySuitcase(items: List<T>): List<T> {
                        if (activeSuitcase == null) return items
                        return items.filter { item ->
                            val id = when(item) {
                                is ShirtEntity -> "SHIRT_${item.id}"
                                is PantEntity -> "PANT_${item.id}"
                                is ShoeEntity -> "SHOE_${item.id}"
                                is JacketEntity -> "JACKET_${item.id}"
                                is DressEntity -> "DRESS_${item.id}"
                                is SkirtEntity -> "SKIRT_${item.id}"
                                is TieEntity -> "TIE_${item.id}"
                                is BagEntity -> "BAG_${item.id}"
                                is WatchEntity -> "WATCH_${item.id}"
                                is CustomGarmentEntity -> "CUSTOM_${item.id}"
                                else -> ""
                            }
                            suitcaseItemIds.contains(id)
                        }
                    }

                    // WAIT: I need to re-check the ID format used in SuitcaseViewModel
                    // In SuitcaseViewModel it was:
                    // is ShirtEntity -> "SHIRT_${entity.id}"
                    // is PantEntity -> "PANT_${entity.id}" (entity.id not entity.itemCode)
                    
                    @Suppress("UNCHECKED_CAST")
                    val filteredShirts = filterBySuitcase(array[1] as List<ShirtEntity>)
                    @Suppress("UNCHECKED_CAST")
                    val filteredPants = filterBySuitcase(array[2] as List<PantEntity>)
                    @Suppress("UNCHECKED_CAST")
                    val filteredShoes = filterBySuitcase(array[3] as List<ShoeEntity>)
                    @Suppress("UNCHECKED_CAST")
                    val filteredTies = filterBySuitcase(array[4] as List<TieEntity>)
                    @Suppress("UNCHECKED_CAST")
                    val filteredWatches = filterBySuitcase(array[5] as List<WatchEntity>)
                    @Suppress("UNCHECKED_CAST")
                    val filteredFragrances = array[6] as List<FragranceEntity> // Fragrances usually not in suitcase
                    @Suppress("UNCHECKED_CAST")
                    val filteredJackets = filterBySuitcase(array[7] as List<JacketEntity>)
                    @Suppress("UNCHECKED_CAST")
                    val filteredBags = filterBySuitcase(array[8] as List<BagEntity>)
                    @Suppress("UNCHECKED_CAST")
                    val filteredDresses = filterBySuitcase(array[9] as List<DressEntity>)
                    @Suppress("UNCHECKED_CAST")
                    val filteredSkirts = filterBySuitcase(array[10] as List<SkirtEntity>)
                    @Suppress("UNCHECKED_CAST")
                    val filteredCustoms = filterBySuitcase(array[11] as List<CustomGarmentEntity>)

                    val garmentsMap = mapOf(
                        "SHIRTS" to filteredShirts,
                        "PANTS" to filteredPants,
                        "SHOES" to filteredShoes,
                        "TIES" to filteredTies,
                        "WATCHES" to filteredWatches,
                        "FRAGRANCES" to filteredFragrances,
                        "JACKETS" to filteredJackets,
                        "BAGS" to filteredBags,
                        "DRESSES" to filteredDresses,
                        "SKIRTS" to filteredSkirts,
                        "PERSONALIZADO" to filteredCustoms
                    )
                    
                    Pair(garmentsMap, activeSuitcase)
                }.collect { (garmentsMap, suitcase) ->
                    _uiState.update { it.copy(
                        availableGarments = garmentsMap,
                        isTripModeActive = suitcase != null,
                        activeTripName = suitcase?.tripName
                    ) }
                }
            }
        }
    }

    fun toggleBaseGarment(garment: Any) {
        _uiState.update { state ->
            val newList = if (state.baseGarments.contains(garment)) {
                state.baseGarments - garment
            } else {
                state.baseGarments + garment
            }
            state.copy(baseGarments = newList)
        }
    }

    fun onInstructionChanged(newInstruction: String) {
        _uiState.update { it.copy(userInstruction = newInstruction) }
    }

    fun consultAiForOutfit() {
        viewModelScope.launch {
            val userInput = _uiState.value.userInstruction
            
            // Determinamos qué instrucción usar: la nueva del campo de texto o la anterior
            val finalInstruction = userInput.ifBlank { _uiState.value.activeInstruction }
            
            // Si el usuario escribió algo nuevo y diferente, reseteamos la lista
            val shouldReset = userInput.isNotBlank() && userInput != _uiState.value.activeInstruction

            _uiState.update { 
                it.copy(
                    isLoading = true, 
                    errorMessage = null, 
                    isOutfitConfirmed = false,
                    recommendations = if (shouldReset) emptyList() else it.recommendations,
                    activeInstruction = finalInstruction,
                    userInstruction = "" // Limpiamos el campo de texto inmediatamente
                ) 
            }

            val available = _uiState.value.availableGarments
            @Suppress("UNCHECKED_CAST")
            val shirts = (available["SHIRTS"] as? List<ShirtEntity>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val pants = (available["PANTS"] as? List<PantEntity>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val shoes = (available["SHOES"] as? List<ShoeEntity>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val ties = (available["TIES"] as? List<TieEntity>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val watches = (available["WATCHES"] as? List<WatchEntity>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val fragrances = (available["FRAGRANCES"] as? List<FragranceEntity>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val jackets = (available["JACKETS"] as? List<JacketEntity>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val bags = (available["BAGS"] as? List<BagEntity>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val dresses = (available["DRESSES"] as? List<DressEntity>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val skirts = (available["SKIRTS"] as? List<SkirtEntity>) ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val customs = (available["PERSONALIZADO"] as? List<CustomGarmentEntity>) ?: emptyList()

            repository.getPrimaryUser().firstOrNull()?.let { user ->
                val userId = user.id
                val activeRules = repository.getActiveStyleRules(userId).firstOrNull() ?: emptyList()

                if (shirts.isEmpty() && dresses.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Se requiere al menos 1 camisa o 1 vestido limpio."
                        )
                    }
                    return@launch
                }

                // Recopilamos los IDs de las prendas ya sugeridas en esta sesión para que la IA no las repita
                val alreadyRecommendedIds = _uiState.value.recommendations.flatMap { outfit ->
                    listOfNotNull(
                        outfit.shirt?.id, outfit.pant?.id, outfit.shoe?.id,
                        outfit.tie?.id, outfit.watch?.id, outfit.fragrance?.id, outfit.jacket?.id,
                        outfit.bag?.id, outfit.dress?.id, outfit.skirt?.id
                    )
                }.map { it.toString() }

                // Solicitamos la recomendación usando la instrucción final (nueva o persistida)
                val aiRecommendation = azureService.generateOutfitRecommendation(
                    shirts = shirts,
                    pants = pants,
                    shoes = shoes,
                    ties = ties,
                    watches = watches,
                    fragrances = fragrances,
                    jackets = jackets,
                    bags = bags,
                    dresses = dresses,
                    skirts = skirts,
                    customGarments = customs,
                    styleRules = activeRules,
                    baseGarments = _uiState.value.baseGarments,
                    userInstruction = finalInstruction,
                    excludeIds = alreadyRecommendedIds
                )

                if (aiRecommendation.startsWith("Error", ignoreCase = true)) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = aiRecommendation
                        )
                    }
                    return@launch
                }

                android.util.Log.d("OutfitParser", "Raw AI Response: $aiRecommendation")

                // Regex ultra-flexible para capturar los IDs en cualquier formato (con o sin espacios, mayúsculas o minúsculas)
                val regex = """SELECCION_IDS:?\s*SHIRT=([^,\s]+),?\s*PANT=([^,\s]+),?\s*SHOE=([^,\s]+),?\s*TIE=([^,\s]+),?\s*WATCH=([^,\s]+),?\s*FRAGRANCE=([^,\s]+),?\s*JACKET=([^,\s]+),?\s*BAG=([^,\s]+),?\s*DRESS=([^,\s]+),?\s*SKIRT=([^,\s.]+)""".toRegex(RegexOption.IGNORE_CASE)
                val matchResult = regex.find(aiRecommendation)
                
                var cleanText = aiRecommendation
                var selectedShirt: ShirtEntity? = null
                var selectedPant: PantEntity? = null
                var selectedShoe: ShoeEntity? = null
                var selectedTie: TieEntity? = null
                var selectedWatch: WatchEntity? = null
                var selectedFragrance: FragranceEntity? = null
                var selectedJacket: JacketEntity? = null
                var selectedBag: BagEntity? = null
                var selectedDress: DressEntity? = null
                var selectedSkirt: SkirtEntity? = null

                matchResult?.let { result ->
                    // Limpiamos el texto eliminando la línea de IDs y cualquier formato markdown residual
                    cleanText = aiRecommendation.replace(result.value, "")
                        .replace(Regex("""#+\s*"""), "") // Quitar encabezados h1, h2...
                        .replace("**", "")
                        .replace("*", "")
                        // Regex ultra-agresiva para IDs: borra (ID: 1), ID 1, Camisa 1, (SHOE 2), etc.
                        .replace(Regex("""\(?\b(ID|SHIRT|PANT|SHOE|TIE|WATCH|FRAGRANCE|JACKET|BAG|DRESS|SKIRT)[:\s]+\d+\)?""", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("""\(\s*\d+\s*\)"""), "") // Borra números entre paréntesis solos (1)
                        .trim()
                    
                    // Función auxiliar para limpiar IDs de caracteres como puntos o espacios
                    fun parseId(value: String): Long? = value.replace(Regex("[^0-9]"), "").toLongOrNull()
                    
                    selectedShirt = shirts.find { it.id == parseId(result.groupValues[1]) }
                    selectedPant = pants.find { it.id == parseId(result.groupValues[2]) }
                    selectedShoe = shoes.find { it.id == parseId(result.groupValues[3]) }
                    selectedTie = ties.find { it.id == parseId(result.groupValues[4]) }
                    selectedWatch = watches.find { it.id == parseId(result.groupValues[5]) }
                    selectedFragrance = fragrances.find { it.id == parseId(result.groupValues[6]) }
                    selectedJacket = jackets.find { it.id == parseId(result.groupValues[7]) }
                    selectedBag = bags.find { it.id == parseId(result.groupValues[8]) }
                    selectedDress = dresses.find { it.id == parseId(result.groupValues[9]) }
                    selectedSkirt = skirts.find { it.id == parseId(result.groupValues[10]) }
                }

                // Búsqueda manual de IDs si el regex falla por formato
                if (matchResult == null && aiRecommendation.contains("SHIRT=", ignoreCase = true)) {
                    android.util.Log.w("OutfitParser", "Regex failed, attempting manual extraction")
                    val tags = listOf("SHIRT", "PANT", "SHOE", "TIE", "WATCH", "FRAGRANCE", "JACKET", "BAG", "DRESS", "SKIRT")
                    tags.forEach { tag ->
                        val idValue = Regex("""$tag=([^\s,.]+)""", RegexOption.IGNORE_CASE)
                            .find(aiRecommendation)?.groupValues?.get(1)
                        
                        val id = idValue?.replace(Regex("[^0-9]"), "")?.toLongOrNull()
                        if (id != null) {
                            when (tag) {
                                "SHIRT" -> selectedShirt = shirts.find { it.id == id }
                                "PANT" -> selectedPant = pants.find { it.id == id }
                                "SHOE" -> selectedShoe = shoes.find { it.id == id }
                                "TIE" -> selectedTie = ties.find { it.id == id }
                                "WATCH" -> selectedWatch = watches.find { it.id == id }
                                "FRAGRANCE" -> selectedFragrance = fragrances.find { it.id == id }
                                "JACKET" -> selectedJacket = jackets.find { it.id == id }
                                "BAG" -> selectedBag = bags.find { it.id == id }
                                "DRESS" -> selectedDress = dresses.find { it.id == id }
                                "SKIRT" -> selectedSkirt = skirts.find { it.id == id }
                            }
                        }
                    }
                    // Limpiamos la línea de IDs del texto final
                    cleanText = aiRecommendation.split(Regex("SELECCION_IDS", RegexOption.IGNORE_CASE))[0].trim()
                }

                // VALIDACIÓN CRÍTICA: Si no pudimos extraer NINGUNA prenda, la respuesta no es válida.
                if (selectedShirt == null && selectedPant == null && selectedShoe == null && selectedDress == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "La IA no pudo seleccionar prendas válidas. Por favor, intenta de nuevo."
                        )
                    }
                    return@launch
                }

                // Fallback de texto si la explicación es muy corta o inexistente
                if (cleanText.length < 10) {
                    cleanText = "Basado en tu inventario, esta es la mejor combinación para hoy considerando el estilo y la armonía de colores."
                }

                val newRecommendation = RecommendedOutfit(
                    text = cleanText,
                    shirt = selectedShirt,
                    pant = selectedPant,
                    shoe = selectedShoe,
                    tie = selectedTie,
                    watch = selectedWatch,
                    fragrance = selectedFragrance,
                    jacket = selectedJacket,
                    bag = selectedBag,
                    dress = selectedDress,
                    skirt = selectedSkirt
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        // Añadimos la nueva recomendación al inicio de la lista
                        recommendations = listOf(newRecommendation) + it.recommendations,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun confirmOutfit(outfit: RecommendedOutfit) {
        viewModelScope.launch {
            repository.getPrimaryUser().firstOrNull()?.let { user ->
                // Incluimos las prendas base que el usuario seleccionó manualmente
                val baseCustoms = _uiState.value.baseGarments.filterIsInstance<CustomGarmentEntity>()
                
                repository.confirmAndWearOutfit(
                    userId = user.id,
                    shirt = outfit.shirt,
                    pant = outfit.pant,
                    shoe = outfit.shoe,
                    tie = outfit.tie,
                    watch = outfit.watch,
                    fragrance = outfit.fragrance,
                    jacket = outfit.jacket,
                    bag = outfit.bag,
                    dress = outfit.dress,
                    skirt = outfit.skirt,
                    customGarments = outfit.customGarments + baseCustoms,
                    summary = outfit.text
                )

                _uiState.update {
                    it.copy(
                        isOutfitConfirmed = true,
                        recommendations = emptyList(),
                        baseGarments = emptyList() // Limpiamos tras confirmar
                    )
                }
            }
        }
    }
}
