package com.myperfectoutfit.ui.state

import com.myperfectoutfit.data.local.entities.*

data class WardrobeUiState(
    val isLoading: Boolean = true,
    val user: UserEntity? = null,
    val selectedCategory: CategoryFilter = CategoryFilter.ALL,
    val shirts: List<ShirtEntity> = emptyList(),
    val pants: List<PantEntity> = emptyList(),
    val shoes: List<ShoeEntity> = emptyList(),
    val ties: List<TieEntity> = emptyList(),
    val watches: List<WatchEntity> = emptyList(),
    val jackets: List<JacketEntity> = emptyList(),
    val fragrances: List<FragranceEntity> = emptyList(),
    val bags: List<BagEntity> = emptyList(),
    val dresses: List<DressEntity> = emptyList(),
    val skirts: List<SkirtEntity> = emptyList(),
    val customCategories: List<CustomCategoryEntity> = emptyList(),
    val customGarments: List<CustomGarmentEntity> = emptyList(),
    val selectedCustomCategoryId: Long? = null,
    val errorMessage: String? = null
)

enum class CategoryFilter(val displayName: String) {
    ALL("Todas"),
    SHIRTS("Camisas"),
    PANTS("Pantalones"),
    SHOES("Zapatos"),
    TIES("Corbatas"),
    WATCHES("Relojes"),
    JACKETS("Chaquetas"),
    FRAGRANCES("Fragancias"),
    BAGS("Bolsos"),
    DRESSES("Vestidos"),
    SKIRTS("Faldas"),
    LAUNDRY("Lavandería")
}