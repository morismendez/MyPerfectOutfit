package com.myperfectoutfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.data.remote.GeminiOutfitService
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
    val userInstruction: String = "",
    val activeInstruction: String = "", // La instrucción que se usó para las recomendaciones actuales
    val recommendations: List<RecommendedOutfit> = emptyList(),
    val baseGarments: List<Any> = emptyList(),
    val availableGarments: Map<String, List<Any>> = emptyMap(),
    val isOutfitConfirmed: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class OutfitViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val geminiService: GeminiOutfitService
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

                combine(
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
                    repository.getAllCustomGarments(userId) // También incluimos personalizadas
                ) { array ->
                    mapOf(
                        "SHIRTS" to array[0],
                        "PANTS" to array[1],
                        "SHOES" to array[2],
                        "TIES" to array[3],
                        "WATCHES" to array[4],
                        "FRAGRANCES" to array[5],
                        "JACKETS" to array[6],
                        "BAGS" to array[7],
                        "DRESSES" to array[8],
                        "SKIRTS" to array[9],
                        "PERSONALIZADO" to array[10]
                    )
                }.collect { garmentsMap ->
                    _uiState.update { it.copy(availableGarments = garmentsMap) }
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
            val finalInstruction = if (userInput.isNotBlank()) userInput else _uiState.value.activeInstruction
            
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

            repository.getPrimaryUser().firstOrNull()?.let { user ->
                val userId = user.id
                val activeCats = user.activeCategories.split(",")

                val shirts = if (activeCats.contains("SHIRTS")) repository.getCleanShirts(userId).firstOrNull() ?: emptyList() else emptyList()
                val pants = if (activeCats.contains("PANTS")) repository.getCleanPants(userId).firstOrNull() ?: emptyList() else emptyList()
                val shoes = if (activeCats.contains("SHOES")) repository.getAvailableShoes(userId).firstOrNull() ?: emptyList() else emptyList()
                val ties = if (activeCats.contains("TIES")) repository.getCleanTies(userId).firstOrNull() ?: emptyList() else emptyList()
                val watches = if (activeCats.contains("WATCHES")) repository.getAvailableWatches(userId).firstOrNull() ?: emptyList() else emptyList()
                val fragrances = if (activeCats.contains("FRAGRANCES")) repository.getAvailableFragrances(userId).firstOrNull() ?: emptyList() else emptyList()
                val jackets = if (activeCats.contains("JACKETS")) repository.getCleanJackets(userId).firstOrNull() ?: emptyList() else emptyList()
                val bags = if (activeCats.contains("BAGS")) repository.getAvailableBags(userId).firstOrNull() ?: emptyList() else emptyList()
                val dresses = if (activeCats.contains("DRESSES")) repository.getCleanDresses(userId).firstOrNull() ?: emptyList() else emptyList()
                val skirts = if (activeCats.contains("SKIRTS")) repository.getCleanSkirts(userId).firstOrNull() ?: emptyList() else emptyList()
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
                val aiRecommendation = geminiService.generateOutfitRecommendation(
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
                    styleRules = activeRules,
                    baseGarments = _uiState.value.baseGarments,
                    userInstruction = finalInstruction,
                    excludeIds = alreadyRecommendedIds,
                    userApiKey = repository.getGeminiApiKey()
                )

                val regex = """SELECCION_IDS: SHIRT=(.*?), PANT=(.*?), SHOE=(.*?), TIE=(.*?), WATCH=(.*?), FRAGRANCE=(.*?), JACKET=(.*?), BAG=(.*?), DRESS=(.*?), SKIRT=(.*)""".toRegex()
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
                    cleanText = aiRecommendation.replace(result.value, "")
                        .replace("**", "").replace("*", "").trim()
                    
                    selectedShirt = shirts.find { it.id == result.groupValues[1].toLongOrNull() }
                    selectedPant = pants.find { it.id == result.groupValues[2].toLongOrNull() }
                    selectedShoe = shoes.find { it.id == result.groupValues[3].toLongOrNull() }
                    selectedTie = ties.find { it.id == result.groupValues[4].toLongOrNull() }
                    selectedWatch = watches.find { it.id == result.groupValues[5].toLongOrNull() }
                    selectedFragrance = fragrances.find { it.id == result.groupValues[6].toLongOrNull() }
                    selectedJacket = jackets.find { it.id == result.groupValues[7].toLongOrNull() }
                    selectedBag = bags.find { it.id == result.groupValues[8].toLongOrNull() }
                    selectedDress = dresses.find { it.id == result.groupValues[9].toLongOrNull() }
                    selectedSkirt = skirts.find { it.id == result.groupValues[10].toLongOrNull() }
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
                        errorMessage = if (aiRecommendation.startsWith("Error")) aiRecommendation else null
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
