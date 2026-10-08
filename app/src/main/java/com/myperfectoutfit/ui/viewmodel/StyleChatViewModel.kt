package com.myperfectoutfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.data.remote.AzureConsultationService
import com.myperfectoutfit.data.repository.PlanningRepository
import com.myperfectoutfit.data.repository.SuitcaseRepository
import com.myperfectoutfit.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import javax.inject.Inject

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val imageUri: android.net.Uri? = null,
    val suggestedItems: List<SuitcaseItem> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class StyleChatUiState(
    val userId: Long = 0,
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class StyleChatViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val planningRepository: PlanningRepository,
    private val suitcaseRepository: SuitcaseRepository,
    private val azureService: AzureConsultationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(StyleChatUiState())
    val uiState: StateFlow<StyleChatUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getPrimaryUser().collect { user ->
                user?.let {
                    _uiState.update { it.copy(userId = user.id) }
                    loadHistory(user.id)
                }
            }
        }
    }

    private fun loadHistory(userId: Long) {
        viewModelScope.launch {
            // Necesitamos el inventario para re-vincular las fotos en el historial
            val allGarmentsFlow = combine(
                repository.getAllShirts(userId), repository.getAllPants(userId),
                repository.getAllShoes(userId), repository.getAllJackets(userId),
                repository.getAllDresses(userId), repository.getAllSkirts(userId),
                repository.getAllTies(userId), repository.getAllWatches(userId),
                repository.getAllBags(userId), repository.getAllFragrances(userId)
            ) { flows -> flows.flatMap { (it as? List<*>) ?: emptyList<Any>() } }

            combine(repository.getChatHistory(userId), allGarmentsFlow) { entities, inventory ->
                entities.map { entity ->
                    val suggestedIds = entity.suggestedGarmentIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
                    val suggestedItems = suggestedIds.mapNotNull { id ->
                        findItemInInventory(id, inventory)
                    }

                    val rawText = entity.text.split("SUGGESTED_IDS:")[0].trim()
                    val cleanedHistoryText = rawText
                        .replace(Regex("""\(?\b(SHIRT|PANT|SHOE|TIE|WATCH|FRAGRANCE|JACKET|BAG|DRESS|SKIRT|CUSTOM)_\d+\)?""", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("""\s\s+"""), " ")
                        .replace(Regex("""\s+([,.;:])"""), "$1")
                        .trim()

                    ChatMessage(
                        text = cleanedHistoryText,
                        isUser = entity.isUser,
                        imageUri = entity.imageUri?.toUri(),
                        suggestedItems = suggestedItems,
                        timestamp = entity.timestamp
                    )
                }
            }.collect { messages ->
                _uiState.update { state ->
                    state.copy(
                        messages = messages.ifEmpty { listOf(getWelcomeMessage()) }
                    )
                }
            }
        }
    }

    private fun findItemInInventory(id: String, inventory: List<Any?>): SuitcaseItem? {
        val typeParts = id.split("_")
        if (typeParts.size < 2) return null
        val type = typeParts[0]
        val numericId = typeParts[1].toLongOrNull() ?: return null
        
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
                is FragranceEntity -> type == "FRAGRANCE" && item.id == numericId
                else -> false
            }
        }

        return entity?.let {
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

    private fun getWelcomeMessage() = ChatMessage(
        text = if (java.util.Locale.getDefault().language == "es") 
            "¡Hola! Soy tu asistente de estilo personal. ¿En qué puedo ayudarte hoy con tu armario o alguna combinación?"
        else 
            "Hello! I am your personal style assistant. How can I help you today with your wardrobe or outfit combinations?",
        isUser = false
    )

    fun sendMessage(text: String, imageUri: android.net.Uri? = null, imageBitmap: android.graphics.Bitmap? = null) {
        val userId = _uiState.value.userId
        if (userId == 0L || (text.isBlank() && imageBitmap == null)) return

        val userMessage = ChatMessage(
            text = text, 
            isUser = true,
            imageUri = imageUri
        )
        
        _uiState.update { it.copy(isLoading = true) }
        
        // Guardar mensaje del usuario en DB
        viewModelScope.launch {
            repository.saveChatMessage(
                ChatMessageEntity(
                    userId = userId,
                    text = userMessage.text,
                    isUser = true,
                    imageUri = userMessage.imageUri?.toString(),
                    timestamp = userMessage.timestamp
                )
            )
        }

        viewModelScope.launch {
            try {
                // 1. Recopilar inventario COMPLETO
                val inventory = combine(
                    repository.getAllShirts(userId),
                    repository.getAllPants(userId),
                    repository.getAllShoes(userId),
                    repository.getAllJackets(userId),
                    repository.getAllDresses(userId),
                    repository.getAllSkirts(userId),
                    repository.getAllTies(userId),
                    repository.getAllWatches(userId),
                    repository.getAllBags(userId),
                    repository.getAllFragrances(userId)
                ) { flows ->
                    mapOf(
                        "Camisas" to flows[0],
                        "Pantalones" to flows[1],
                        "Zapatos" to flows[2],
                        "Chaquetas" to flows[3],
                        "Vestidos" to flows[4],
                        "Faldas" to flows[5],
                        "Corbatas" to flows[6],
                        "Relojes" to flows[7],
                        "Bolsos" to flows[8],
                        "Fragancias" to flows[9]
                    )
                }.first()

                // 2. Obtener historial reciente, planes futuros y maletas
                val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val nextWeek = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 7) }
                val nextWeekStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(nextWeek.time)

                val history = repository.getFullHistory(userId).first().take(20)
                val planning = planningRepository.getPlannedOutfitsInRange(userId, today, nextWeekStr).first()
                val suitcases = suitcaseRepository.getAllSuitcases(userId).first().filter { !it.isCompleted }

                // 3. Obtener reglas de estilo
                val rules = repository.getActiveStyleRules(userId).firstOrNull() ?: emptyList()

                // 4. Preparar historial de chat (últimos 6 mensajes)
                val chatHistory = _uiState.value.messages.takeLast(6).map { it.text to it.isUser }

                // 5. Consultar a la IA
                val responseRaw = azureService.getStyleAdvice(
                    userQuery = text,
                    inventory = inventory,
                    styleRules = rules,
                    chatHistory = chatHistory,
                    usageHistory = history,
                    futurePlanning = planning,
                    activeSuitcases = suitcases,
                    imageBitmap = imageBitmap
                )
                
                // Parsear sugerencias
                val parts = responseRaw.split("SUGGESTED_IDS:")
                val responseCleanText = parts[0].trim()
                
                val suggestedIdsFromTag = if (parts.size > 1) {
                    parts[1].split(",").map { it.trim() }.filter { it.isNotBlank() }
                } else emptyList()

                // Extraer también cualquier ID que la IA haya mencionado directamente en el texto
                val inlineIdsRegex = Regex("""\b(SHIRT|PANT|SHOE|TIE|WATCH|FRAGRANCE|JACKET|BAG|DRESS|SKIRT|CUSTOM)_\d+\b""", RegexOption.IGNORE_CASE)
                val inlineIds = inlineIdsRegex.findAll(responseCleanText).map { it.value.uppercase() }.toList()

                val allSuggestedIds = (suggestedIdsFromTag + inlineIds).distinct()

                // Limpiar del texto visible cualquier aparición de IDs como (SHIRT_2), [SHOE_1], SHIRT_2, etc.
                val finalDisplayText = responseCleanText
                    .replace(Regex("""\(?\b(SHIRT|PANT|SHOE|TIE|WATCH|FRAGRANCE|JACKET|BAG|DRESS|SKIRT|CUSTOM)_\d+\)?""", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("""\s\s+"""), " ")
                    .replace(Regex("""\s+([,.;:])"""), "$1")
                    .trim()

                val inventoryList = inventory.values.flatten()
                val suggestedItems = allSuggestedIds.mapNotNull { id ->
                    findItemInInventory(id, inventoryList)
                }

                val aiResponse = ChatMessage(
                    text = finalDisplayText, 
                    isUser = false,
                    suggestedItems = suggestedItems
                )

                // Guardar respuesta de la IA en DB
                repository.saveChatMessage(
                    ChatMessageEntity(
                        userId = userId,
                        text = responseRaw,
                        isUser = false,
                        suggestedGarmentIds = allSuggestedIds.joinToString(","),
                        timestamp = aiResponse.timestamp
                    )
                )

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                android.util.Log.e("StyleChat", "Error consulting AI: ${e.message}")
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        errorMessage = if (java.util.Locale.getDefault().language == "es") 
                            "Ocurrió un error al procesar tu consulta."
                        else 
                            "An error occurred while processing your request."
                    ) 
                }
            }
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
        "FRAGRANCE" -> "Fragancia"
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
        is FragranceEntity -> "${entity.brand} ${entity.name}"
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
        is FragranceEntity -> entity.imageUrl
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

    fun clearChat() {
        viewModelScope.launch {
            val userId = _uiState.value.userId
            if (userId != 0L) {
                repository.clearChatHistory(userId)
            }
        }
    }
}
