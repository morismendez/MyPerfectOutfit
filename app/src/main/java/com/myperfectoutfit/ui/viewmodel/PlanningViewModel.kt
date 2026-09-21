package com.myperfectoutfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myperfectoutfit.data.local.dao.PlannedOutfitWithDetails
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.data.local.enums.LaundryState
import com.myperfectoutfit.data.remote.AzurePlanningService
import com.myperfectoutfit.data.repository.PlanningRepository
import com.myperfectoutfit.data.repository.SuitcaseRepository
import com.myperfectoutfit.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class PlanningUiState(
    val plannedOutfits: List<PlannedOutfitWithDetails> = emptyList(),
    val proposedOutfits: List<PlannedOutfitWithDetails> = emptyList(), // Para revisión antes de guardar
    val isLoading: Boolean = false,
    val userId: Long = 1L,
    val selectedDate: Calendar = Calendar.getInstance(),
    val availableGarments: Map<String, List<Any>> = emptyMap(),
    val isTripModeActive: Boolean = false,
    val activeTripName: String? = null,
    val userInstruction: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class PlanningViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val planningRepository: PlanningRepository,
    private val suitcaseRepository: SuitcaseRepository,
    private val azurePlanningService: AzurePlanningService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanningUiState())
    val uiState: StateFlow<PlanningUiState> = _uiState.asStateFlow()

    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        viewModelScope.launch {
            repository.getPrimaryUser().collect { user ->
                user?.let { 
                    _uiState.update { it.copy(userId = user.id) }
                    loadData()
                }
            }
        }
    }

    private fun loadData() {
        loadPlannedOutfits()
        loadAllGarments()
    }

    private fun loadPlannedOutfits() {
        val state = _uiState.value
        val dateStr = sdf.format(state.selectedDate.time)
        
        viewModelScope.launch {
            planningRepository.getPlannedOutfitByDate(state.userId, dateStr)
                .collect { outfit ->
                    _uiState.update { it.copy(plannedOutfits = listOfNotNull(outfit)) }
                }
        }
    }

    private fun loadAllGarments() {
        viewModelScope.launch {
            val userId = _uiState.value.userId
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

            combine(
                suitcaseRepository.getActiveSuitcase(userId, today),
                repository.getAllShirts(userId),
                repository.getAllPants(userId),
                repository.getAllShoes(userId),
                repository.getAllTies(userId),
                repository.getAllWatches(userId),
                repository.getAllFragrances(userId),
                repository.getAllJackets(userId),
                repository.getAllBags(userId),
                repository.getAllDresses(userId),
                repository.getAllSkirts(userId),
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

                val garmentsMap = mapOf(
                    "SHIRTS" to filterBySuitcase(array[1] as List<ShirtEntity>),
                    "PANTS" to filterBySuitcase(array[2] as List<PantEntity>),
                    "SHOES" to filterBySuitcase(array[3] as List<ShoeEntity>),
                    "TIES" to filterBySuitcase(array[4] as List<TieEntity>),
                    "WATCHES" to filterBySuitcase(array[5] as List<WatchEntity>),
                    "FRAGRANCES" to (array[6] as List<FragranceEntity>),
                    "JACKETS" to filterBySuitcase(array[7] as List<JacketEntity>),
                    "BAGS" to filterBySuitcase(array[8] as List<BagEntity>),
                    "DRESSES" to filterBySuitcase(array[9] as List<DressEntity>),
                    "SKIRTS" to filterBySuitcase(array[10] as List<SkirtEntity>),
                    "CUSTOM" to filterBySuitcase(array[11] as List<CustomGarmentEntity>)
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

    fun moveDate(days: Int) {
        _uiState.update { 
            val newCal = it.selectedDate.clone() as Calendar
            newCal.add(Calendar.DAY_OF_YEAR, days)
            it.copy(selectedDate = newCal)
        }
        loadPlannedOutfits()
    }

    fun savePlannedOutfit(
        date: String,
        shirt: ShirtEntity? = null,
        pant: PantEntity? = null,
        shoe: ShoeEntity? = null,
        tie: TieEntity? = null,
        watch: WatchEntity? = null,
        fragrance: FragranceEntity? = null,
        jacket: JacketEntity? = null,
        bag: BagEntity? = null,
        dress: DressEntity? = null,
        skirt: SkirtEntity? = null,
        customGarments: List<CustomGarmentEntity> = emptyList()
    ) {
        viewModelScope.launch {
            val entity = PlannedOutfitEntity(
                userId = _uiState.value.userId,
                dateString = date,
                shirtId = shirt?.id,
                pantId = pant?.id,
                shoeId = shoe?.id,
                tieId = tie?.id,
                watchId = watch?.id,
                fragranceId = fragrance?.id,
                jacketId = jacket?.id,
                bagId = bag?.id,
                dressId = dress?.id,
                skirtId = skirt?.id,
                customGarmentIds = customGarments.joinToString(",") { it.id.toString() }
            )
            planningRepository.savePlannedOutfit(entity)
        }
    }

    fun deletePlannedOutfit(outfit: PlannedOutfitEntity) {
        viewModelScope.launch {
            planningRepository.deletePlannedOutfit(outfit)
        }
    }

    fun confirmPlannedOutfit(outfitDetails: PlannedOutfitWithDetails) {
        viewModelScope.launch {
            // Obtener prendas personalizadas si existen
            val customGarments = if (!outfitDetails.plannedOutfit.customGarmentIds.isNullOrBlank()) {
                val ids = outfitDetails.plannedOutfit.customGarmentIds.split(",").mapNotNull { it.toLongOrNull() }
                repository.getAllCustomGarments(_uiState.value.userId).first().filter { ids.contains(it.id) }
            } else emptyList()

            repository.confirmAndWearOutfit(
                userId = _uiState.value.userId,
                shirt = outfitDetails.shirt,
                pant = outfitDetails.pant,
                shoe = outfitDetails.shoe,
                tie = outfitDetails.tie,
                watch = outfitDetails.watch,
                fragrance = outfitDetails.fragrance,
                jacket = outfitDetails.jacket,
                bag = outfitDetails.bag,
                dress = outfitDetails.dress,
                skirt = outfitDetails.skirt,
                customGarments = customGarments,
                summary = outfitDetails.plannedOutfit.notes ?: "Outfit planeado para el ${outfitDetails.plannedOutfit.dateString}"
            )
            // Eliminar de la planificación una vez usado
            planningRepository.deletePlannedOutfit(outfitDetails.plannedOutfit)
        }
    }

    fun onInstructionChanged(instruction: String) {
        _uiState.update { it.copy(userInstruction = instruction) }
    }

    fun generateAiPlanningForRange(startMillis: Long, endMillis: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val state = _uiState.value
            val userId = state.userId
            
            // Usar Calendar UTC para calcular la diferencia exacta de días
            val startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = startMillis }
            val endCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = endMillis }
            
            // Resetear horas para asegurar cálculo exacto por días
            startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0); startCal.set(Calendar.SECOND, 0); startCal.set(Calendar.MILLISECOND, 0)
            endCal.set(Calendar.HOUR_OF_DAY, 0); endCal.set(Calendar.MINUTE, 0); endCal.set(Calendar.SECOND, 0); endCal.set(Calendar.MILLISECOND, 0)

            val days = ((endCal.timeInMillis - startCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() + 1
            val startDate = Date(startMillis)

            // 1. Obtener inventario necesario
            val shirts = repository.getAllShirts(userId).first()
            val pants = repository.getAllPants(userId).first()
            val shoes = repository.getAllShoes(userId).first()
            val ties = repository.getAllTies(userId).first()
            val watches = repository.getAllWatches(userId).first()
            val fragrances = repository.getAllFragrances(userId).first()
            val jackets = repository.getAllJackets(userId).first()
            val bags = repository.getAllBags(userId).first()
            val dresses = repository.getAllDresses(userId).first()
            val skirts = repository.getAllSkirts(userId).first()

            val response = azurePlanningService.generateMultiDayPlanning(
                daysCount = days,
                startDate = startDate,
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
                customGarments = repository.getAllCustomGarments(userId).first().filter { it.laundryState == LaundryState.CLEAN },
                userInstruction = state.userInstruction,
                styleRules = repository.getActiveStyleRules(userId).first()
            )

            if (response.startsWith("Error")) {
                _uiState.update { it.copy(isLoading = false, errorMessage = response) }
                return@launch
            }

            val proposed = parseAiPlanningResponse(response, shirts, pants, shoes, ties, watches, fragrances, jackets, bags, dresses, skirts)
            _uiState.update { it.copy(isLoading = false, proposedOutfits = proposed, userInstruction = "") }
        }
    }

    private fun parseAiPlanningResponse(
        response: String,
        shirts: List<ShirtEntity>,
        pants: List<PantEntity>,
        shoes: List<ShoeEntity>,
        ties: List<TieEntity>,
        watches: List<WatchEntity>,
        fragrances: List<FragranceEntity>,
        jackets: List<JacketEntity>,
        bags: List<BagEntity>,
        dresses: List<DressEntity>,
        skirts: List<SkirtEntity>
    ): List<PlannedOutfitWithDetails> {
        val proposedList = mutableListOf<PlannedOutfitWithDetails>()
        val daysBlocks = response.split(Regex("FECHA:", RegexOption.IGNORE_CASE)).filter { it.isNotBlank() }
        
        daysBlocks.forEach { block ->
            try {
                val lines = block.lines()
                val dateStr = lines[0].trim()
                val notes = lines.find { it.contains("NOTAS:", ignoreCase = true) }?.split(":")?.getOrNull(1)?.trim()
                val idsLine = lines.find { it.contains("IDS:", ignoreCase = true) } ?: ""

                fun parseId(tag: String): Long? {
                    return Regex("$tag=([^,\\s]+)", RegexOption.IGNORE_CASE)
                        .find(idsLine)?.groupValues?.get(1)
                        ?.replace(Regex("[^0-9]"), "")?.toLongOrNull()
                }

                val shirt = parseId("SHIRT")?.let { id -> shirts.find { it.id == id } }
                val pant = parseId("PANT")?.let { id -> pants.find { it.id == id } }
                val shoe = parseId("SHOE")?.let { id -> shoes.find { it.id == id } }
                val tie = parseId("TIE")?.let { id -> ties.find { it.id == id } }
                val watch = parseId("WATCH")?.let { id -> watches.find { it.id == id } }
                val fragrance = parseId("FRAGRANCE")?.let { id -> fragrances.find { it.id == id } }
                val jacket = parseId("JACKET")?.let { id -> jackets.find { it.id == id } }
                val bag = parseId("BAG")?.let { id -> bags.find { it.id == id } }
                val dress = parseId("DRESS")?.let { id -> dresses.find { it.id == id } }
                val skirt = parseId("SKIRT")?.let { id -> skirts.find { it.id == id } }

                val entity = PlannedOutfitEntity(
                    userId = _uiState.value.userId,
                    dateString = dateStr,
                    shirtId = shirt?.id,
                    pantId = pant?.id,
                    shoeId = shoe?.id,
                    tieId = tie?.id,
                    watchId = watch?.id,
                    fragranceId = fragrance?.id,
                    jacketId = jacket?.id,
                    bagId = bag?.id,
                    dressId = dress?.id,
                    skirtId = skirt?.id,
                    notes = notes
                )
                
                proposedList.add(PlannedOutfitWithDetails(
                    plannedOutfit = entity,
                    shirt = shirt,
                    pant = pant,
                    shoe = shoe,
                    tie = tie,
                    watch = watch,
                    fragrance = fragrance,
                    jacket = jacket,
                    bag = bag,
                    dress = dress,
                    skirt = skirt
                ))
            } catch (e: Exception) {
                android.util.Log.e("PlanningParser", "Error parsing day block: ${e.message}")
            }
        }
        return proposedList
    }

    fun confirmSelectedOutfits(selectedOutfits: List<PlannedOutfitWithDetails>) {
        viewModelScope.launch {
            selectedOutfits.forEach { proposed ->
                planningRepository.savePlannedOutfit(proposed.plannedOutfit)
            }
            _uiState.update { it.copy(proposedOutfits = emptyList()) }
        }
    }

    fun discardProposedOutfits() {
        _uiState.update { it.copy(proposedOutfits = emptyList()) }
    }

    // Lógica de conflictos
    fun getConflicts(outfit: PlannedOutfitWithDetails): List<String> {
        val conflicts = mutableListOf<String>()
        val today = sdf.format(Date())
        val isFuture = outfit.plannedOutfit.dateString > today
        
        fun checkGarment(name: String, laundryState: LaundryState?) {
            if (laundryState == LaundryState.IN_LAUNDRY) {
                conflicts.add(if (isFuture) "Pendiente: $name está en lavandería." else "Conflicto: $name está sucio hoy.")
            }
        }

        fun checkAvailability(name: String, isAvailable: Boolean?) {
            if (isAvailable == false) {
                conflicts.add(if (isFuture) "Pendiente: $name no está disponible." else "Conflicto: $name no disponible hoy.")
            }
        }

        checkGarment("Camisa", outfit.shirt?.laundryState)
        checkGarment("Pantalón", outfit.pant?.laundryState)
        checkGarment("Chaqueta", outfit.jacket?.laundryState)
        checkGarment("Corbata", outfit.tie?.laundryState)
        checkGarment("Vestido", outfit.dress?.laundryState)
        checkGarment("Falda", outfit.skirt?.laundryState)
        
        checkAvailability("Zapatos", outfit.shoe?.isAvailable)
        checkAvailability("Reloj", outfit.watch?.isAvailable)
        checkAvailability("Bolso", outfit.bag?.isAvailable)
        checkAvailability("Fragancia", outfit.fragrance?.isAvailable)

        return conflicts
    }
}
