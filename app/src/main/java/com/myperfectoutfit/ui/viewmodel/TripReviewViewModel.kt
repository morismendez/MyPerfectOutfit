package com.myperfectoutfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myperfectoutfit.data.local.entities.SuitcaseEntity
import com.myperfectoutfit.data.local.entities.CustomGarmentEntity
import com.myperfectoutfit.data.local.entities.ShirtEntity
import com.myperfectoutfit.data.local.entities.PantEntity
import com.myperfectoutfit.data.local.entities.ShoeEntity
import com.myperfectoutfit.data.local.entities.JacketEntity
import com.myperfectoutfit.data.local.entities.DressEntity
import com.myperfectoutfit.data.local.entities.SkirtEntity
import com.myperfectoutfit.data.local.entities.TieEntity
import com.myperfectoutfit.data.local.entities.BagEntity
import com.myperfectoutfit.data.local.entities.WatchEntity
import com.myperfectoutfit.data.local.enums.LaundryState
import com.myperfectoutfit.data.repository.SuitcaseRepository
import com.myperfectoutfit.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class TripReviewUiState(
    val finishedTripToReview: SuitcaseEntity? = null,
    val garmentsToReview: List<SuitcaseItem> = emptyList(),
    val isProcessing: Boolean = false
)

@HiltViewModel
class TripReviewViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val suitcaseRepository: SuitcaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TripReviewUiState())
    val uiState: StateFlow<TripReviewUiState> = _uiState.asStateFlow()

    init {
        checkAllTrips()
    }

    private fun checkAllTrips() {
        viewModelScope.launch {
            repository.getPrimaryUser().collect { user ->
                user?.let {
                    suitcaseRepository.getAllSuitcases(user.id).collect { list ->
                        checkForFinishedTrips(list)
                    }
                }
            }
        }
    }

    private fun checkForFinishedTrips(list: List<SuitcaseEntity>) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val finishedTrip = list.find { !it.isCompleted && it.endDate < today }
        
        if (finishedTrip != null && _uiState.value.finishedTripToReview == null) {
            prepareTripReview(finishedTrip)
        }
    }

    private fun prepareTripReview(suitcase: SuitcaseEntity) {
        viewModelScope.launch {
            // Cargar prendas de la maleta para la revisión
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
            val items = garmentData.mapNotNull { data ->
                val id = data.split(":")[0]
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
                        markAsDirty = shouldMarkAsDirtyByDefault(type)
                    )
                }
            }

            _uiState.update { it.copy(finishedTripToReview = suitcase, garmentsToReview = items) }
        }
    }

    private fun shouldMarkAsDirtyByDefault(type: String): Boolean {
        return when(type) {
            "SHIRT", "PANT", "DRESS", "SKIRT", "CUSTOM" -> true
            else -> false
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

    fun toggleReviewItem(itemId: String) {
        _uiState.update { state ->
            val newList = state.garmentsToReview.map {
                if (it.id == itemId) it.copy(markAsDirty = !it.markAsDirty) else it
            }
            state.copy(garmentsToReview = newList)
        }
    }

    fun confirmTripReview() {
        viewModelScope.launch {
            val state = _uiState.value
            val trip = state.finishedTripToReview ?: return@launch
            
            _uiState.update { it.copy(isProcessing = true) }

            // Recargar inventario para tener los objetos actuales
            val inventory = combine(
                repository.getAllShirts(trip.userId),
                repository.getAllPants(trip.userId),
                repository.getAllShoes(trip.userId),
                repository.getAllJackets(trip.userId),
                repository.getAllDresses(trip.userId),
                repository.getAllSkirts(trip.userId),
                repository.getAllTies(trip.userId),
                repository.getAllBags(trip.userId),
                repository.getAllWatches(trip.userId),
                repository.getAllCustomGarments(trip.userId)
            ) { flows ->
                flows.flatMap { it as List<*> }
            }.first()

            // Actualizar lavandería en el repositorio principal
            state.garmentsToReview.forEach { item ->
                if (item.markAsDirty) {
                    val parts = item.id.split("_")
                    val type = parts[0]
                    val id = parts[1].toLongOrNull() ?: return@forEach
                    
                    when(type) {
                        "SHIRT" -> repository.updateShirtState(id, LaundryState.IN_LAUNDRY)
                        "PANT" -> repository.updatePantState(id, LaundryState.IN_LAUNDRY)
                        "TIE" -> repository.updateTieState(id, LaundryState.IN_LAUNDRY)
                        "JACKET" -> repository.updateJacketState(id, LaundryState.IN_LAUNDRY)
                        "DRESS" -> repository.updateDressState(id, LaundryState.IN_LAUNDRY)
                        "SKIRT" -> repository.updateSkirtState(id, LaundryState.IN_LAUNDRY)
                        "CUSTOM" -> {
                            val entity = inventory.find { 
                                (it as? CustomGarmentEntity)?.id == id 
                            } as? CustomGarmentEntity
                            entity?.let { repository.updateCustomGarment(it.copy(laundryState = LaundryState.IN_LAUNDRY)) }
                        }
                    }
                }
            }

            // Marcar maleta como completada
            suitcaseRepository.saveSuitcase(trip.copy(isCompleted = true))
            
            _uiState.update { it.copy(finishedTripToReview = null, garmentsToReview = emptyList(), isProcessing = false) }
        }
    }

    fun dismissTripReview() {
        _uiState.update { it.copy(finishedTripToReview = null, garmentsToReview = emptyList()) }
    }
}
