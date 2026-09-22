package com.myperfectoutfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.data.remote.AzureSuitcaseService
import com.myperfectoutfit.data.repository.SuitcaseRepository
import com.myperfectoutfit.data.repository.WardrobeRepository
import com.myperfectoutfit.ui.enums.SuitcaseType
import com.myperfectoutfit.ui.enums.TripStyle
import com.myperfectoutfit.ui.enums.SpecialEvent
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class SuitcaseItem(
    val id: String,
    val name: String,
    val category: String,
    val categoryDisplay: String = "",
    val imageUrl: String? = null,
    val isDirty: Boolean = false,
    val isPacked: Boolean = false,
    val markAsDirty: Boolean = false
)

data class SuitcaseTask(
    val name: String,
    val isChecked: Boolean = false
)

data class SuitcaseAiResponse(
    val justification: String,
    val selectedGarmentIds: List<String>,
    val missingSuggestions: List<String>,
    val essentials: List<String>
)

data class ReplacementAiResponse(
    val selectedId: String,
    val reason: String
)

data class SuitcaseUiState(
    val isLoading: Boolean = false,
    val isReplacingAi: Boolean = false,
    val pendingAiSuggestion: SuitcaseItem? = null,
    val aiSuggestionReason: String? = null,
    val userId: Long = 1L,
    val tripName: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null,
    val suitcaseType: SuitcaseType = SuitcaseType.CARRY_ON,
    val selectedStyles: Set<TripStyle> = setOf(TripStyle.CASUAL),
    val selectedEvents: Set<SpecialEvent> = emptySet(),
    val customEvent: String = "",
    val aiJustification: String? = null,
    val selectedGarments: List<SuitcaseItem> = emptyList(),
    val fullInventory: Map<String, List<Any>> = emptyMap(),
    val missingSuggestions: List<SuitcaseTask> = emptyList(),
    val essentials: List<SuitcaseTask> = emptyList(),
    val suitcases: List<SuitcaseEntity> = emptyList(),
    val currentSuitcaseId: Long? = null,
    val isSaved: Boolean = false,
    val isCompleted: Boolean = false,
    val isPackingMode: Boolean = false,
    val dateOverlapWarning: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SuitcaseViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val suitcaseRepository: SuitcaseRepository,
    private val azureSuitcaseService: AzureSuitcaseService
) : ViewModel() {

    private val _uiState = MutableStateFlow(SuitcaseUiState())
    val uiState: StateFlow<SuitcaseUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getPrimaryUser().collect { user ->
                user?.let {
                    _uiState.update { it.copy(userId = user.id) }
                    loadSuitcases()
                }
            }
        }
    }

    private fun loadSuitcases() {
        viewModelScope.launch {
            suitcaseRepository.getAllSuitcases(_uiState.value.userId).collect { list ->
                _uiState.update { it.copy(suitcases = list) }
            }
        }
    }

    fun deleteSuitcase(suitcase: SuitcaseEntity) {
        viewModelScope.launch {
            suitcaseRepository.deleteSuitcase(suitcase)
            if (_uiState.value.currentSuitcaseId == suitcase.id) {
                clearForm()
            }
        }
    }

    fun updateTripDetails(name: String, start: Long?, end: Long?) {
        _uiState.update { it.copy(tripName = name, startDate = start, endDate = end) }
        checkDateOverlap()
    }

    private fun checkDateOverlap() {
        val state = _uiState.value
        val start = state.startDate ?: return
        val end = state.endDate ?: return
        val currentId = state.currentSuitcaseId

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val overlappingTrip = state.suitcases.find { suitcase ->
            if (suitcase.id == currentId) return@find false
            
            val sDate = try { sdf.parse(suitcase.startDate)?.time } catch (_: Exception) { null }
            val eDate = try { sdf.parse(suitcase.endDate)?.time } catch (_: Exception) { null }
            
            if (sDate != null && eDate != null) {
                // Overlap: (StartA <= EndB) and (EndA >= StartB)
                (start <= eDate) && (end >= sDate)
            } else false
        }

        _uiState.update { 
            it.copy(
                dateOverlapWarning = overlappingTrip?.let { trip ->
                    "Atención: Estas fechas se solapan con tu viaje '${trip.tripName}'"
                }
            )
        }
    }

    fun updateSuitcaseType(type: SuitcaseType) {
        _uiState.update { it.copy(suitcaseType = type) }
    }

    fun toggleTripStyle(style: TripStyle) {
        _uiState.update { state ->
            val newStyles = if (state.selectedStyles.contains(style)) {
                if (state.selectedStyles.size > 1) state.selectedStyles - style else state.selectedStyles
            } else {
                state.selectedStyles + style
            }
            state.copy(selectedStyles = newStyles)
        }
    }

    fun updateCustomEvent(text: String) {
        _uiState.update { it.copy(customEvent = text) }
    }

    fun toggleSpecialEvent(event: SpecialEvent) {
        _uiState.update { state ->
            val newEvents = if (state.selectedEvents.contains(event)) {
                state.selectedEvents - event
            } else {
                state.selectedEvents + event
            }
            state.copy(selectedEvents = newEvents)
        }
    }

    fun togglePackingMode() {
        _uiState.update { it.copy(isPackingMode = !it.isPackingMode) }
    }

    fun toggleItemPacked(itemId: String) {
        _uiState.update { state ->
            val newList = state.selectedGarments.map { 
                if (it.id == itemId) it.copy(isPacked = !it.isPacked) else it
            }
            state.copy(selectedGarments = newList)
        }
    }

    fun toggleEssentialChecked(name: String) {
        _uiState.update { state ->
            val newList = state.essentials.map { 
                if (it.name == name) it.copy(isChecked = !it.isChecked) else it
            }
            state.copy(essentials = newList)
        }
    }

    fun toggleSuggestionChecked(name: String) {
        _uiState.update { state ->
            val newList = state.missingSuggestions.map { 
                if (it.name == name) it.copy(isChecked = !it.isChecked) else it
            }
            state.copy(missingSuggestions = newList)
        }
    }

    fun replaceGarment(oldId: String, newGarment: SuitcaseItem) {
        _uiState.update { state ->
            val newList = state.selectedGarments.map { 
                if (it.id == oldId) newGarment else it
            }
            state.copy(selectedGarments = newList)
        }
    }

    fun requestAiReplacement(garmentToReplace: SuitcaseItem) {
        viewModelScope.launch {
            _uiState.update { it.copy(isReplacingAi = true, pendingAiSuggestion = null, aiSuggestionReason = null) }
            val state = _uiState.value
            
            val alternatives = getAlternativesFor(garmentToReplace.category, garmentToReplace.id)
            if (alternatives.isEmpty()) {
                _uiState.update { it.copy(isReplacingAi = false) }
                return@launch
            }

            val tripContext = "VIAJE: ${state.tripName}, ESTILO: ${state.selectedStyles.joinToString { it.title }}"
            val currentSuitcase = state.selectedGarments.joinToString { "${it.name} (${it.categoryDisplay})" }
            val alternativesStr = alternatives.joinToString("\n") { "ID:${it.id} - ${it.name}" }

            val responseStr = azureSuitcaseService.getReplacementRecommendation(
                tripContext = tripContext,
                currentSuitcase = currentSuitcase,
                garmentToReplace = "${garmentToReplace.name} (${garmentToReplace.categoryDisplay})",
                alternatives = alternativesStr,
                userLanguage = Locale.getDefault().displayLanguage
            )

            if (!responseStr.startsWith("Error")) {
                try {
                    val result = Gson().fromJson(responseStr, ReplacementAiResponse::class.java)
                    val newGarment = alternatives.find { it.id == result.selectedId }
                    if (newGarment != null) {
                        _uiState.update { it.copy(pendingAiSuggestion = newGarment, aiSuggestionReason = result.reason) }
                    }
                } catch (_: Exception) {}
            }
            
            _uiState.update { it.copy(isReplacingAi = false) }
        }
    }

    fun confirmAiReplacement(oldId: String) {
        val suggestion = _uiState.value.pendingAiSuggestion ?: return
        replaceGarment(oldId, suggestion)
        clearAiSuggestion()
    }

    fun clearAiSuggestion() {
        _uiState.update { it.copy(pendingAiSuggestion = null, aiSuggestionReason = null) }
    }

    fun getAlternativesFor(category: String, currentId: String): List<SuitcaseItem> {
        val inventory = _uiState.value.fullInventory[category] ?: return emptyList()
        val otherSelectedIds = _uiState.value.selectedGarments
            .map { it.id }
            .filter { it != currentId }
            .toSet()

        return inventory.mapNotNull { entity ->
            val id = when(entity) {
                is ShirtEntity -> "SHIRT_${entity.id}"
                is PantEntity -> "PANT_${entity.id}"
                is ShoeEntity -> "SHOE_${entity.id}"
                is JacketEntity -> "JACKET_${entity.id}"
                is DressEntity -> "DRESS_${entity.id}"
                is SkirtEntity -> "SKIRT_${entity.id}"
                is TieEntity -> "TIE_${entity.id}"
                is BagEntity -> "BAG_${entity.id}"
                is WatchEntity -> "WATCH_${entity.id}"
                is CustomGarmentEntity -> "CUSTOM_${entity.id}"
                else -> null
            } ?: return@mapNotNull null

            if (otherSelectedIds.contains(id)) return@mapNotNull null

            SuitcaseItem(
                id = id,
                category = category,
                categoryDisplay = getCategoryDisplayName(category),
                name = getEntityName(entity),
                imageUrl = getEntityImageUrl(entity),
                isDirty = isEntityDirty(entity)
            )
        }
    }

    private fun getCategoryDisplayName(type: String) = when(type) {
        "SHIRT" -> "Camisa"
        "PANT" -> "Pantalón"
        "SHOE" -> "Calzado"
        "JACKET" -> "Abrigo / Chaqueta"
        "DRESS" -> "Vestido"
        "SKIRT" -> "Falda"
        "TIE" -> "Corbata"
        "BAG" -> "Bolso"
        "WATCH" -> "Reloj"
        "CUSTOM" -> "Personalizado"
        else -> type
    }

    private fun getEntityName(entity: Any) = when(entity) {
        is ShirtEntity -> "${entity.brand} ${entity.primaryColor}"
        is PantEntity -> "${entity.brand ?: ""} ${entity.primaryColor}"
        is ShoeEntity -> "${entity.brand} ${entity.color}"
        is JacketEntity -> "${entity.brand ?: ""} ${entity.color}"
        is DressEntity -> "${entity.brand ?: ""} ${entity.color}"
        is SkirtEntity -> "${entity.brand ?: ""} ${entity.color}"
        is TieEntity -> "${entity.colorRange} ${entity.pattern}"
        is BagEntity -> "${entity.brand ?: ""} ${entity.color}"
        is WatchEntity -> "${entity.brand} ${entity.model}"
        is CustomGarmentEntity -> entity.attributeValues
        else -> "Prenda"
    }

    private fun getEntityImageUrl(entity: Any) = when(entity) {
        is ShirtEntity -> entity.imageUrl
        is PantEntity -> entity.imageUrl
        is ShoeEntity -> entity.imageUrl
        is JacketEntity -> entity.imageUrl
        is DressEntity -> entity.imageUrl
        is SkirtEntity -> entity.imageUrl
        is TieEntity -> entity.imageUrl
        is BagEntity -> entity.imageUrl
        is WatchEntity -> entity.imageUrl
        is CustomGarmentEntity -> entity.imageUrl
        else -> null
    }

    private fun isEntityDirty(entity: Any): Boolean {
        val state = when(entity) {
            is ShirtEntity -> entity.laundryState
            is PantEntity -> entity.laundryState
            is JacketEntity -> entity.laundryState
            is DressEntity -> entity.laundryState
            is SkirtEntity -> entity.laundryState
            is TieEntity -> entity.laundryState
            is CustomGarmentEntity -> entity.laundryState
            else -> null
        }
        return state != null && state != com.myperfectoutfit.data.local.enums.LaundryState.CLEAN
    }

    fun generateSuitcase() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.startDate == null || state.endDate == null || state.tripName.isBlank()) {
                _uiState.update { it.copy(errorMessage = "Completa los datos del viaje") }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val days = ((state.endDate - state.startDate) / (1000 * 60 * 60 * 24)).toInt() + 1
            
            // Obtener inventario COMPLETO para la IA
            val inventory = combine(
                repository.getAllShirts(state.userId),
                repository.getAllPants(state.userId),
                repository.getAllShoes(state.userId),
                repository.getAllJackets(state.userId),
                repository.getAllDresses(state.userId),
                repository.getAllSkirts(state.userId),
                repository.getAllTies(state.userId),
                repository.getAllBags(state.userId),
                repository.getAllWatches(state.userId),
                repository.getAllCustomGarments(state.userId)
            ) { flows ->
                mapOf(
                    "SHIRT" to flows[0],
                    "PANT" to flows[1],
                    "SHOE" to flows[2],
                    "JACKET" to flows[3],
                    "DRESS" to flows[4],
                    "SKIRT" to flows[5],
                    "TIE" to flows[6],
                    "BAG" to flows[7],
                    "WATCH" to flows[8],
                    "CUSTOM" to flows[9]
                )
            }.first()

            val responseStr = azureSuitcaseService.generateSuitcaseRecommendation(
                tripName = state.tripName,
                days = days,
                suitcaseType = state.suitcaseType.name,
                tripStyle = state.selectedStyles.joinToString { it.name },
                specialEvents = (state.selectedEvents.map { it.name } + if (state.customEvent.isNotBlank()) listOf("CUSTOM:${state.customEvent}") else emptyList()).joinToString(),
                inventory = inventory,
                userLanguage = Locale.getDefault().displayLanguage
            )

            if (responseStr.startsWith("Error")) {
                _uiState.update { it.copy(isLoading = false, errorMessage = responseStr) }
            } else {
                try {
                    val gson = Gson()
                    val aiResponse = gson.fromJson(responseStr, SuitcaseAiResponse::class.java)
                    
                    // Mapear IDs a items con detalles
                    val allItemsList = inventory.values.flatten()
                    val selectedItems = aiResponse.selectedGarmentIds.mapNotNull { id ->
                        val parts = id.split("_")
                        if (parts.size < 2) return@mapNotNull null
                        val type = parts[0]
                        val numericId = parts[1].toLongOrNull() ?: return@mapNotNull null
                        
                        val entity = allItemsList.find { item ->
                            when(item) {
                                is ShirtEntity -> type == "SHIRT" && item.id == numericId
                                is PantEntity -> type == "PANT" && item.id == numericId
                                is ShoeEntity -> type == "SHOE" && item.id == numericId
                                is JacketEntity -> type == "JACKET" && item.id == numericId
                                is DressEntity -> type == "DRESS" && item.id == numericId
                                is SkirtEntity -> type == "SKIRT" && item.id == numericId
                                is TieEntity -> type == "TIE" && item.id == numericId
                                is BagEntity -> type == "BAG" && item.id == numericId
                                is WatchEntity -> type == "WATCH" && item.id == numericId
                                is CustomGarmentEntity -> type == "CUSTOM" && item.id == numericId
                                else -> false
                            }
                        }

                        entity?.let {
                            SuitcaseItem(
                                id = id,
                                category = type,
                                categoryDisplay = getCategoryDisplayName(type),
                                name = getEntityName(it),
                                imageUrl = getEntityImageUrl(it),
                                isDirty = isEntityDirty(it)
                            )
                        }
                    }

                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            fullInventory = inventory,
                            aiJustification = aiResponse.justification,
                            selectedGarments = selectedItems,
                            missingSuggestions = aiResponse.missingSuggestions.map { name -> SuitcaseTask(name) },
                            essentials = aiResponse.essentials.map { name -> SuitcaseTask(name) }
                        )
                    }
                } catch (_: Exception) {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Error al procesar respuesta IA") }
                }
            }
        }
    }

    fun saveSuitcase() {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.tripName.isBlank() || state.startDate == null || state.endDate == null) return@launch

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val entity = SuitcaseEntity(
                id = state.currentSuitcaseId ?: 0L,
                userId = state.userId,
                tripName = state.tripName,
                startDate = sdf.format(Date(state.startDate)),
                endDate = sdf.format(Date(state.endDate)),
                suitcaseType = state.suitcaseType.name,
                tripStyle = state.selectedStyles.joinToString { it.name },
                specialEvents = (state.selectedEvents.map { it.name } + if (state.customEvent.isNotBlank()) listOf("CUSTOM:${state.customEvent}") else emptyList()).joinToString(","),
                garmentIds = state.selectedGarments.joinToString(",") { "${it.id}:${it.isPacked}" },
                missingSuggestions = state.missingSuggestions.joinToString("|") { "${it.name}:${it.isChecked}" },
                essentials = state.essentials.joinToString("|") { "${it.name}:${it.isChecked}" },
                aiSuggestions = state.aiJustification
            )

            if (state.currentSuitcaseId != null) {
                suitcaseRepository.saveSuitcase(entity) // saveSuitcase uses insert with REPLACE strategy in Dao but let's be sure
            } else {
                val newId = suitcaseRepository.saveSuitcase(entity)
                _uiState.update { it.copy(currentSuitcaseId = newId) }
            }
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun resetSaveStatus() {
        _uiState.update { it.copy(isSaved = false) }
    }

    fun clearForm() {
        _uiState.update { state ->
            state.copy(
                currentSuitcaseId = null,
                tripName = "",
                startDate = null,
                endDate = null,
                suitcaseType = SuitcaseType.CARRY_ON,
                selectedStyles = setOf(TripStyle.CASUAL),
                selectedEvents = emptySet(),
                customEvent = "",
                aiJustification = null,
                selectedGarments = emptyList(),
                missingSuggestions = emptyList(),
                essentials = emptyList(),
                errorMessage = null
            )
        }
    }

    fun loadSavedSuitcase(suitcase: SuitcaseEntity) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            
            val start = try { sdf.parse(suitcase.startDate)?.time } catch (_: Exception) { null }
            val end = try { sdf.parse(suitcase.endDate)?.time } catch (_: Exception) { null }

            val styles = suitcase.tripStyle.split(",")
                .mapNotNull { name -> TripStyle.entries.find { it.name == name } }
                .toSet()

            val events = suitcase.specialEvents.split(",")
                .mapNotNull { name -> 
                    if (name.startsWith("CUSTOM:")) null 
                    else SpecialEvent.entries.find { it.name == name } 
                }
                .toSet()
            
            val customEvent = suitcase.specialEvents.split(",")
                .find { it.startsWith("CUSTOM:") }
                ?.removePrefix("CUSTOM:") ?: ""

            _uiState.update { state ->
                state.copy(
                    currentSuitcaseId = suitcase.id,
                    tripName = suitcase.tripName,
                    startDate = start,
                    endDate = end,
                    suitcaseType = SuitcaseType.valueOf(suitcase.suitcaseType),
                    selectedStyles = styles.ifEmpty { state.selectedStyles },
                    selectedEvents = events,
                    customEvent = customEvent,
                    aiJustification = suitcase.aiSuggestions,
                    missingSuggestions = suitcase.missingSuggestions?.split("|")?.mapNotNull { 
                        val p = it.split(":")
                        if (p[0].isBlank()) null else SuitcaseTask(p[0], p.getOrNull(1)?.toBoolean() ?: false)
                    } ?: emptyList(),
                    essentials = suitcase.essentials?.split("|")?.mapNotNull { 
                        val p = it.split(":")
                        if (p[0].isBlank()) null else SuitcaseTask(p[0], p.getOrNull(1)?.toBoolean() ?: false)
                    } ?: emptyList(),
                    selectedGarments = emptyList(),
                    isCompleted = suitcase.isCompleted,
                    isLoading = suitcase.garmentIds.isNotBlank()
                )
            }

            if (suitcase.garmentIds.isNotBlank()) {
                val inventory = combine(
                    repository.getAllShirts(suitcase.userId),
                    repository.getAllPants(suitcase.userId),
                    repository.getAllShoes(suitcase.userId),
                    repository.getAllJackets(suitcase.userId),
                    repository.getAllDresses(suitcase.userId),
                    repository.getAllSkirts(suitcase.userId),
                    repository.getAllTies(suitcase.userId),
                    repository.getAllBags(suitcase.userId),
                    repository.getAllWatches(suitcase.userId),
                    repository.getAllCustomGarments(suitcase.userId)
                ) { flows ->
                    flows.flatMap { it as List<*> }
                }.first()

                val garmentData = suitcase.garmentIds.split(",")
                val loadedItems = garmentData.mapNotNull { data ->
                    val dataParts = data.split(":")
                    val id = dataParts[0]
                    val isPacked = dataParts.getOrNull(1)?.toBoolean() ?: false

                    val parts = id.split("_")
                    if (parts.size < 2) return@mapNotNull null
                    val type = parts[0]
                    val numericId = parts[1].toLongOrNull() ?: return@mapNotNull null
                    
                    val entity = inventory.find { item ->
                        when(item) {
                            is ShirtEntity -> type == "SHIRT" && item.id == numericId
                            is PantEntity -> type == "PANT" && item.id == numericId
                            is ShoeEntity -> type == "SHOE" && item.id == numericId
                            is JacketEntity -> type == "JACKET" && item.id == numericId
                            is DressEntity -> type == "DRESS" && item.id == numericId
                            is SkirtEntity -> type == "SKIRT" && item.id == numericId
                            is TieEntity -> type == "TIE" && item.id == numericId
                            is BagEntity -> type == "BAG" && item.id == numericId
                            is WatchEntity -> type == "WATCH" && item.id == numericId
                            is CustomGarmentEntity -> type == "CUSTOM" && item.id == numericId
                            else -> false
                        }
                    }

                    entity?.let {
                        SuitcaseItem(
                            id = id,
                            category = type,
                            categoryDisplay = getCategoryDisplayName(type),
                            name = getEntityName(it),
                            imageUrl = getEntityImageUrl(it),
                            isDirty = isEntityDirty(it),
                            isPacked = isPacked
                        )
                    }
                }
                
                _uiState.update { it.copy(selectedGarments = loadedItems, isLoading = false) }
            }
        }
    }
}
