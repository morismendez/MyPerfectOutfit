package com.myperfectoutfit.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myperfectoutfit.data.local.entities.*
import com.myperfectoutfit.data.repository.WardrobeRepository
import com.myperfectoutfit.ui.state.CategoryFilter
import com.myperfectoutfit.ui.state.WardrobeUiState
import com.myperfectoutfit.data.remote.GeminiOutfitService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WardrobeViewModel @Inject constructor(
    private val repository: WardrobeRepository,
    private val geminiService: GeminiOutfitService
) : ViewModel() {

    // Filtro seleccionado en la UI
    private val _selectedCategory = MutableStateFlow(CategoryFilter.ALL)
    private val _selectedCustomCategoryId = MutableStateFlow<Long?>(null)

    // Estado reactivo unificado
    val uiState: StateFlow<WardrobeUiState> = repository.getPrimaryUser()
        .flatMapLatest { user ->
            val userId = user?.id ?: 1L

            combine(
                _selectedCategory,
                _selectedCustomCategoryId,
                repository.getAllShirts(userId),
                repository.getAllPants(userId),
                repository.getAllShoes(userId),
                repository.getAllTies(userId),
                repository.getAllWatches(userId),
                repository.getAllJackets(userId),
                repository.getAllFragrances(userId),
                repository.getAllBags(userId),
                repository.getAllDresses(userId),
                repository.getAllSkirts(userId),
                repository.getCustomCategories(userId),
                repository.getAllCustomGarments(userId)
            ) { array ->
                val category = array[0] as CategoryFilter
                val customCatId = array[1] as? Long
                val shirts = array[2] as List<ShirtEntity>
                val pants = array[3] as List<PantEntity>
                val shoes = array[4] as List<ShoeEntity>
                val ties = array[5] as List<TieEntity>
                val watches = array[6] as List<WatchEntity>
                val jackets = array[7] as List<JacketEntity>
                val fragrances = array[8] as List<FragranceEntity>
                val bags = array[9] as List<BagEntity>
                val dresses = array[10] as List<DressEntity>
                val skirts = array[11] as List<SkirtEntity>
                val customCategories = array[12] as List<CustomCategoryEntity>
                val customGarments = array[13] as List<CustomGarmentEntity>

                WardrobeUiState(
                    isLoading = false,
                    user = user,
                    selectedCategory = category,
                    selectedCustomCategoryId = customCatId,
                    shirts = shirts,
                    pants = pants,
                    shoes = shoes,
                    ties = ties,
                    watches = watches,
                    jackets = jackets,
                    fragrances = fragrances,
                    bags = bags,
                    dresses = dresses,
                    skirts = skirts,
                    customCategories = customCategories,
                    customGarments = customGarments
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WardrobeUiState(isLoading = true)
        )

    fun selectCategory(category: CategoryFilter) {
        _selectedCategory.value = category
        _selectedCustomCategoryId.value = null
    }

    fun selectCustomCategory(categoryId: Long) {
        _selectedCategory.value = CategoryFilter.ALL // Reseteamos el filtro básico
        _selectedCustomCategoryId.value = categoryId
    }

    // --- Métodos de inserción adaptados a listas ---

    fun insertShirt(shirt: ShirtEntity) {
        viewModelScope.launch {
            repository.insertShirts(listOf(shirt))
        }
    }

    fun insertPant(pant: PantEntity) {
        viewModelScope.launch {
            repository.insertPants(listOf(pant))
        }
    }

    fun insertShoe(shoe: ShoeEntity) {
        viewModelScope.launch {
            repository.insertShoes(listOf(shoe))
        }
    }

    fun insertTie(tie: TieEntity) {
        viewModelScope.launch {
            repository.insertTies(listOf(tie))
        }
    }

    fun insertWatch(watch: WatchEntity) {
        viewModelScope.launch {
            repository.insertWatches(listOf(watch))
        }
    }

    fun insertFragrance(fragrance: FragranceEntity) {
        viewModelScope.launch {
            repository.insertFragrances(listOf(fragrance))
        }
    }

    fun insertJacket(jacket: JacketEntity) {
        viewModelScope.launch {
            repository.insertJackets(listOf(jacket))
        }
    }

    fun insertBag(bag: BagEntity) {
        viewModelScope.launch {
            repository.insertBags(listOf(bag))
        }
    }

    fun insertDress(dress: DressEntity) {
        viewModelScope.launch {
            repository.insertDresses(listOf(dress))
        }
    }

    fun insertSkirt(skirt: SkirtEntity) {
        viewModelScope.launch {
            repository.insertSkirts(listOf(skirt))
        }
    }

    fun insertCustomCategory(name: String, attributeNames: String, needsLaundry: Boolean) {
        viewModelScope.launch {
            val userId = uiState.value.user?.id ?: 1L
            repository.insertCustomCategory(
                CustomCategoryEntity(
                    name = name, 
                    attributeNames = attributeNames, 
                    userId = userId,
                    needsLaundry = needsLaundry
                )
            )
        }
    }

    fun updateCustomCategory(category: CustomCategoryEntity) {
        viewModelScope.launch {
            repository.updateCustomCategory(category)
        }
    }

    fun deleteCustomCategory(category: CustomCategoryEntity) {
        viewModelScope.launch {
            // DETALLE CRÍTICO: Si la categoría eliminada es la que está seleccionada,
            // debemos resetear la selección a "ALL" antes de borrarla para evitar que 
            // la pantalla quede en blanco (mostrando un filtro que ya no existe).
            if (_selectedCustomCategoryId.value == category.id) {
                _selectedCustomCategoryId.value = null
                _selectedCategory.value = CategoryFilter.ALL
            }
            repository.deleteCustomCategory(category)
        }
    }

    fun insertCustomGarment(garment: CustomGarmentEntity) {
        viewModelScope.launch {
            repository.insertCustomGarment(garment)
        }
    }

    fun updateCustomGarment(garment: CustomGarmentEntity) {
        viewModelScope.launch {
            repository.updateCustomGarment(garment)
        }
    }

    fun deleteCustomGarment(garment: CustomGarmentEntity) {
        viewModelScope.launch {
            repository.deleteCustomGarment(garment)
        }
    }

    // --- Métodos de actualización ---

    fun updateShirt(shirt: ShirtEntity) {
        viewModelScope.launch {
            repository.updateShirt(shirt)
        }
    }

    fun updatePant(pant: PantEntity) {
        viewModelScope.launch {
            repository.updatePant(pant)
        }
    }

    fun updateShoe(shoe: ShoeEntity) {
        viewModelScope.launch {
            repository.updateShoe(shoe)
        }
    }

    fun updateTie(tie: TieEntity) {
        viewModelScope.launch {
            repository.updateTie(tie)
        }
    }

    fun updateWatch(watch: WatchEntity) {
        viewModelScope.launch {
            repository.updateWatch(watch)
        }
    }

    fun updateFragrance(fragrance: FragranceEntity) {
        viewModelScope.launch {
            repository.updateFragrance(fragrance)
        }
    }

    fun updateJacket(jacket: JacketEntity) {
        viewModelScope.launch {
            repository.updateJacket(jacket)
        }
    }

    fun updateBag(bag: BagEntity) {
        viewModelScope.launch {
            repository.updateBag(bag)
        }
    }

    fun updateDress(dress: DressEntity) {
        viewModelScope.launch {
            repository.updateDress(dress)
        }
    }

    fun updateSkirt(skirt: SkirtEntity) {
        viewModelScope.launch {
            repository.updateSkirt(skirt)
        }
    }

    fun deleteShirt(shirt: ShirtEntity) {
    viewModelScope.launch {
        repository.deleteShirt(shirt)
    }
}

fun deletePant(pant: PantEntity) {
    viewModelScope.launch {
        repository.deletePant(pant)
    }
}

fun deleteShoe(shoe: ShoeEntity) {
    viewModelScope.launch {
        repository.deleteShoe(shoe)
    }
}

fun deleteTie(tie: TieEntity) {
    viewModelScope.launch {
        repository.deleteTie(tie)
    }
}

fun deleteWatch(watch: WatchEntity) {
    viewModelScope.launch {
        repository.deleteWatch(watch)
    }
}

fun deleteFragrance(fragrance: FragranceEntity) {
    viewModelScope.launch {
        repository.deleteFragrance(fragrance)
    }
}
fun deleteJacket(jacket: JacketEntity) {
    viewModelScope.launch { repository.deleteJacket(jacket) }
}

fun deleteBag(bag: BagEntity) {
    viewModelScope.launch { repository.deleteBag(bag) }
}

fun deleteDress(dress: DressEntity) {
    viewModelScope.launch { repository.deleteDress(dress) }
}

fun deleteSkirt(skirt: SkirtEntity) {
    viewModelScope.launch { repository.deleteSkirt(skirt) }
}

suspend fun analyzeGarment(
    bitmap: android.graphics.Bitmap, 
    categoryName: String? = null,
    customAttributes: String? = null
): String? {
    val userKey = repository.getGeminiApiKey()
    return geminiService.analyzeGarmentImage(bitmap, categoryName, customAttributes, userKey)
}
}