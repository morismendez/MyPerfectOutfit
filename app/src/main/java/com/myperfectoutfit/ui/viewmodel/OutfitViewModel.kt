package com.myperfectoutfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.data.remote.AzureOutfitService
import com.myperfectoutfit.data.repository.SuitcaseRepository
import com.myperfectoutfit.data.repository.WardrobeRepository
import com.myperfectoutfit.domain.model.GarmentItem
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
    val baseGarments: List<GarmentItem> = emptyList(),
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
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())

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

    fun toggleBaseGarment(garment: GarmentItem) {
        _uiState.update { state ->
            val newList = if (state.baseGarments.contains(garment)) {
                state.baseGarments - garment
            } else {
                state.baseGarments + garment
            }
            state.copy(baseGarments = newList)
        }
    }

    fun toggleBaseGarmentRaw(rawEntity: Any) {
        GarmentItem.fromEntity(rawEntity)?.let { toggleBaseGarment(it) }
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

                // Recopilamos las combinaciones completas ya sugeridas en esta sesión para que la IA no las repita
                val previousOutfitsList = _uiState.value.recommendations.map { outfit ->
                    val customItems = outfit.customGarments.map { 
                        "CUSTOM_${it.id} (${it.attributeValues.replace("|", " ")})".trim() 
                    }
                    val items = listOfNotNull(
                        outfit.shirt?.let { "SHIRT_${it.id} (${it.brand ?: ""} ${it.primaryColor})".trim() },
                        outfit.pant?.let { "PANT_${it.id} (${it.brand ?: ""} ${it.primaryColor})".trim() },
                        outfit.shoe?.let { "SHOE_${it.id} (${it.brand ?: ""} ${it.color})".trim() },
                        outfit.dress?.let { "DRESS_${it.id} (${it.brand ?: ""} ${it.color})".trim() },
                        outfit.skirt?.let { "SKIRT_${it.id} (${it.brand ?: ""} ${it.color})".trim() },
                        outfit.jacket?.let { "JACKET_${it.id} (${it.brand ?: ""} ${it.color})".trim() },
                        outfit.watch?.let { "WATCH_${it.id} (${it.brand} ${it.model})".trim() },
                        outfit.fragrance?.let { "FRAGRANCE_${it.id} (${it.brand} ${it.name})".trim() },
                        outfit.bag?.let { "BAG_${it.id} (${it.brand ?: ""} ${it.color})".trim() },
                        outfit.tie?.let { "TIE_${it.id} (${it.colorRange})".trim() }
                    ) + customItems
                    items.joinToString(" + ")
                }

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
                    previousOutfits = previousOutfitsList
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

                fun extractTagId(tag: String): Long? {
                    val regex = Regex("""\b$tag\s*[:=]\s*(?:[A-Z]+_)?([0-9]+)\b""", RegexOption.IGNORE_CASE)
                    val match = regex.find(aiRecommendation)
                    return match?.groupValues?.get(1)?.toLongOrNull()
                }

                fun extractCustomTagIds(rawText: String): List<Long> {
                    val regex = Regex("""\bCUSTOM\s*[:=]\s*([0-9,\s]+)""", RegexOption.IGNORE_CASE)
                    val match = regex.find(rawText)
                    val strVal = match?.groupValues?.get(1) ?: return emptyList()
                    return strVal.split(",").mapNotNull { it.replace(Regex("[^0-9]"), "").toLongOrNull() }
                }

                var selectedShirt: ShirtEntity? = shirts.find { it.id == extractTagId("SHIRT") }
                var selectedPant: PantEntity? = pants.find { it.id == extractTagId("PANT") }
                var selectedShoe: ShoeEntity? = shoes.find { it.id == extractTagId("SHOE") }
                var selectedTie: TieEntity? = ties.find { it.id == extractTagId("TIE") }
                var selectedWatch: WatchEntity? = watches.find { it.id == extractTagId("WATCH") }
                var selectedFragrance: FragranceEntity? = fragrances.find { it.id == extractTagId("FRAGRANCE") }
                var selectedJacket: JacketEntity? = jackets.find { it.id == extractTagId("JACKET") }
                var selectedBag: BagEntity? = bags.find { it.id == extractTagId("BAG") }
                var selectedDress: DressEntity? = dresses.find { it.id == extractTagId("DRESS") }
                var selectedSkirt: SkirtEntity? = skirts.find { it.id == extractTagId("SKIRT") }

                val selectedCustomIds = extractCustomTagIds(aiRecommendation)
                var selectedCustoms: List<CustomGarmentEntity> = customs.filter { selectedCustomIds.contains(it.id) }

                // Garantizar prendas base elegidas explícitamente por el usuario
                _uiState.value.baseGarments.forEach { garment ->
                    when (garment) {
                        is GarmentItem.Shirt -> if (selectedShirt == null) selectedShirt = garment.entity
                        is GarmentItem.Pant -> if (selectedPant == null) selectedPant = garment.entity
                        is GarmentItem.Shoe -> if (selectedShoe == null) selectedShoe = garment.entity
                        is GarmentItem.Tie -> if (selectedTie == null) selectedTie = garment.entity
                        is GarmentItem.Watch -> if (selectedWatch == null) selectedWatch = garment.entity
                        is GarmentItem.Fragrance -> if (selectedFragrance == null) selectedFragrance = garment.entity
                        is GarmentItem.Jacket -> if (selectedJacket == null) selectedJacket = garment.entity
                        is GarmentItem.Bag -> if (selectedBag == null) selectedBag = garment.entity
                        is GarmentItem.Dress -> if (selectedDress == null) selectedDress = garment.entity
                        is GarmentItem.Skirt -> if (selectedSkirt == null) selectedSkirt = garment.entity
                        is GarmentItem.Custom -> {
                            if (!selectedCustoms.any { c -> c.id == garment.entity.id }) {
                                selectedCustoms = selectedCustoms + garment.entity
                            }
                        }
                    }
                }

                // GARANTIZAR OUTFIT COMPLETO (Pantalón/Falda y Calzado) si existen en el armario del usuario
                if (selectedDress == null) {
                    if (selectedPant == null && selectedSkirt == null) {
                        if (pants.isNotEmpty()) {
                            selectedPant = pants.firstOrNull()
                        } else if (skirts.isNotEmpty()) {
                            selectedSkirt = skirts.firstOrNull()
                        }
                    }
                    if (selectedShoe == null && shoes.isNotEmpty()) {
                        selectedShoe = shoes.firstOrNull()
                    }
                } else {
                    if (selectedShoe == null && shoes.isNotEmpty()) {
                        selectedShoe = shoes.firstOrNull()
                    }
                }

                var cleanText = if (aiRecommendation.contains("SELECCION_IDS", ignoreCase = true)) {
                    val parts = aiRecommendation.split(Regex("SELECCION_IDS", RegexOption.IGNORE_CASE))
                    parts[0].ifBlank { parts.getOrNull(1) ?: "" }
                } else {
                    aiRecommendation
                }

                cleanText = cleanText
                    .replace(Regex("""SELECCION_IDS:.*""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""#+\s*"""), "")
                    .replace("**", "")
                    .replace("*", "")
                    .replace(Regex("""\(?\b(ID|SHIRT|PANT|SHOE|TIE|WATCH|FRAGRANCE|JACKET|BAG|DRESS|SKIRT|CUSTOM)[:=\s]+(?:[A-Z]+_)?\d+\)?""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\(\s*\d+\s*\)"""), "")
                    .trim()

                // VALIDACIÓN CRÍTICA: Si no pudimos extraer NINGUNA prenda, la respuesta no es válida.
                if (selectedShirt == null && selectedPant == null && selectedShoe == null && selectedDress == null && selectedSkirt == null && selectedCustoms.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "La IA no pudo seleccionar prendas válidas. Por favor, intenta de nuevo."
                        )
                    }
                    return@launch
                }

                // Fallback dinámico específico de la combinación seleccionada si el texto de la IA es corto o inexistente
                if (cleanText.length < 15) {
                    val parts = mutableListOf<String>()
                    selectedShirt?.let { parts.add("${it.subType} ${it.brand ?: ""} ${it.primaryColor}".trim()) }
                    selectedDress?.let { parts.add("Vestido ${it.brand ?: ""} ${it.color}".trim()) }
                    selectedPant?.let { parts.add("pantalón ${it.brand ?: ""} ${it.primaryColor}".trim()) }
                    selectedSkirt?.let { parts.add("falda ${it.brand ?: ""} ${it.color}".trim()) }
                    selectedShoe?.let { parts.add("calzado ${it.brand ?: ""} ${it.color}".trim()) }
                    selectedJacket?.let { parts.add("chaqueta ${it.brand ?: ""} ${it.color}".trim()) }
                    selectedWatch?.let { parts.add("reloj ${it.brand} ${it.model}".trim()) }
                    selectedFragrance?.let { parts.add("fragancia ${it.brand} ${it.name}".trim()) }
                    selectedCustoms.forEach { parts.add(it.attributeValues.replace("|", " ")) }

                    cleanText = if (parts.isNotEmpty()) {
                        "Propuesta equilibrada combinando " + parts.joinToString(", ") + ". Selección refinada para lucir elegante y coordinado hoy."
                    } else {
                        "Basado en tu inventario, esta es la mejor combinación para hoy considerando el estilo y la armonía de colores."
                    }
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
                    skirt = selectedSkirt,
                    customGarments = selectedCustoms
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
                val baseCustoms = _uiState.value.baseGarments.filterIsInstance<GarmentItem.Custom>().map { it.entity }
                
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
