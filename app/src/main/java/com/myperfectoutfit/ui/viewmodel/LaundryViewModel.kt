package com.myperfectoutfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.data.local.enums.LaundryState
import com.myperfectoutfit.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LaundryUiState(
    val laundryShirts: List<ShirtEntity> = emptyList(),
    val laundryPants: List<PantEntity> = emptyList(),
    val laundryTies: List<TieEntity> = emptyList(),
    val laundryJackets: List<JacketEntity> = emptyList(),
    val laundryDresses: List<DressEntity> = emptyList(),
    val laundrySkirts: List<SkirtEntity> = emptyList(),
    val laundryCustomGarments: List<CustomGarmentEntity> = emptyList(),
    val isLoading: Boolean = true
) {
    val isEmpty: Boolean
        get() = laundryShirts.isEmpty() && laundryPants.isEmpty() && laundryTies.isEmpty() 
                && laundryJackets.isEmpty() && laundryDresses.isEmpty() && laundrySkirts.isEmpty()
                && laundryCustomGarments.isEmpty()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LaundryViewModel @Inject constructor(
    private val repository: WardrobeRepository
) : ViewModel() {

    val uiState: StateFlow<LaundryUiState> = repository.getPrimaryUser()
        .flatMapLatest { user ->
            if (user == null) flowOf(LaundryUiState(isLoading = false))
            else {
                val activeCats = user.activeCategories.split(",")
                
                combine(
                    repository.getAllShirts(user.id),
                    repository.getAllPants(user.id),
                    repository.getAllTies(user.id),
                    repository.getAllJackets(user.id),
                    repository.getAllDresses(user.id),
                    repository.getAllSkirts(user.id),
                    repository.getAllCustomGarments(user.id)
                ) { array ->
                    val shirts = array[0] as List<ShirtEntity>
                    val pants = array[1] as List<PantEntity>
                    val ties = array[2] as List<TieEntity>
                    val jackets = array[3] as List<JacketEntity>
                    val dresses = array[4] as List<DressEntity>
                    val skirts = array[5] as List<SkirtEntity>
                    val customs = array[6] as List<CustomGarmentEntity>

                    LaundryUiState(
                        laundryShirts = if (activeCats.contains("SHIRTS")) shirts.filter { it.laundryState == LaundryState.IN_LAUNDRY } else emptyList(),
                        laundryPants = if (activeCats.contains("PANTS")) pants.filter { it.laundryState == LaundryState.IN_LAUNDRY } else emptyList(),
                        laundryTies = if (activeCats.contains("TIES")) ties.filter { it.laundryState == LaundryState.IN_LAUNDRY } else emptyList(),
                        laundryJackets = if (activeCats.contains("JACKETS")) jackets.filter { it.laundryState == LaundryState.IN_LAUNDRY } else emptyList(),
                        laundryDresses = if (activeCats.contains("DRESSES")) dresses.filter { it.laundryState == LaundryState.IN_LAUNDRY } else emptyList(),
                        laundrySkirts = if (activeCats.contains("SKIRTS")) skirts.filter { it.laundryState == LaundryState.IN_LAUNDRY } else emptyList(),
                        laundryCustomGarments = customs.filter { it.laundryState == LaundryState.IN_LAUNDRY },
                        isLoading = false
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LaundryUiState()
        )

    fun markShirtClean(shirtId: Long) {
        viewModelScope.launch {
            repository.updateShirtState(shirtId, LaundryState.CLEAN)
        }
    }

    fun markPantClean(pantId: Long) {
        viewModelScope.launch {
            repository.updatePantState(pantId, LaundryState.CLEAN)
        }
    }

    fun markTieClean(tieId: Long) {
        viewModelScope.launch {
            repository.updateTieState(tieId, LaundryState.CLEAN)
        }
    }

    fun markJacketClean(jacketId: Long) {
        viewModelScope.launch {
            repository.updateJacketState(jacketId, LaundryState.CLEAN)
        }
    }

    fun markDressClean(dressId: Long) {
        viewModelScope.launch {
            repository.updateDressState(dressId, LaundryState.CLEAN)
        }
    }

    fun markSkirtClean(skirtId: Long) {
        viewModelScope.launch {
            repository.updateSkirtState(skirtId, LaundryState.CLEAN)
        }
    }

    fun markCustomGarmentClean(garment: CustomGarmentEntity) {
        viewModelScope.launch {
            repository.updateCustomGarment(garment.copy(laundryState = LaundryState.CLEAN))
        }
    }

    fun markAllAsClean() {
        viewModelScope.launch {
            val state = uiState.value
            state.laundryShirts.forEach { repository.updateShirtState(it.id, LaundryState.CLEAN) }
            state.laundryPants.forEach { repository.updatePantState(it.id, LaundryState.CLEAN) }
            state.laundryTies.forEach { repository.updateTieState(it.id, LaundryState.CLEAN) }
            state.laundryJackets.forEach { repository.updateJacketState(it.id, LaundryState.CLEAN) }
            state.laundryDresses.forEach { repository.updateDressState(it.id, LaundryState.CLEAN) }
            state.laundrySkirts.forEach { repository.updateSkirtState(it.id, LaundryState.CLEAN) }
            state.laundryCustomGarments.forEach { repository.updateCustomGarment(it.copy(laundryState = LaundryState.CLEAN)) }
        }
    }
}
