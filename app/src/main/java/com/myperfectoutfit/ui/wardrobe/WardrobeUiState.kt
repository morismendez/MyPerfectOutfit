package com.myperfectoutfit.ui.state

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
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

enum class CategoryFilter(val displayName: String, val icon: ImageVector) {
    ALL("Todas", Icons.Default.GridView),
    SHIRTS("Camisas", Icons.Default.Checkroom),
    PANTS("Pantalones", Icons.Default.VerticalSplit),
    SHOES("Zapatos", Icons.AutoMirrored.Filled.DirectionsWalk),
    TIES("Corbatas", Icons.Default.MilitaryTech),
    WATCHES("Relojes", Icons.Default.Watch),
    JACKETS("Chaquetas", Icons.Default.DryCleaning),
    FRAGRANCES("Fragancias", Icons.Default.AutoAwesome),
    BAGS("Bolsos", Icons.Default.ShoppingBag),
    DRESSES("Vestidos", Icons.Default.Woman),
    SKIRTS("Faldas", Icons.Default.HorizontalSplit),
    LAUNDRY("Lavandería", Icons.Default.LocalLaundryService)
}
