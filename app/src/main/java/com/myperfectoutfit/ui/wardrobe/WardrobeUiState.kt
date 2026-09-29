package com.myperfectoutfit.ui.state

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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

object CustomIcons {
    val Shirt: ImageVector by lazy {
        ImageVector.Builder(
            name = "Shirt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).materialPath {
            moveTo(16f, 4f)
            lineTo(14f, 4f)
            curveTo(14f, 5.5f, 13f, 6.5f, 12f, 6.5f)
            curveTo(11f, 6.5f, 10f, 5.5f, 10f, 4f)
            lineTo(8f, 4f)
            lineTo(3f, 7.5f)
            lineTo(5.5f, 11f)
            lineTo(8f, 9.5f)
            lineTo(8f, 20f)
            lineTo(16f, 20f)
            lineTo(16f, 9.5f)
            lineTo(18.5f, 11f)
            lineTo(21f, 7.5f)
            close()
        }.build()
    }

    val Pants: ImageVector by lazy {
        ImageVector.Builder(
            name = "Pants",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).materialPath {
            moveTo(6f, 4f)
            horizontalLineTo(18f)
            lineTo(19f, 20f)
            horizontalLineTo(13.5f)
            lineTo(12f, 11f)
            lineTo(10.5f, 20f)
            horizontalLineTo(5f)
            close()
        }.build()
    }

    val Shoes: ImageVector by lazy {
        ImageVector.Builder(
            name = "Shoes",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).materialPath {
            moveTo(21f, 13.5f)
            curveTo(21f, 11.5f, 19f, 10.5f, 16.5f, 10.5f)
            curveTo(14.5f, 10.5f, 12.5f, 12f, 11f, 13.5f)
            lineTo(7.5f, 15f)
            curveTo(4.5f, 15.5f, 2.5f, 16.5f, 2.5f, 18f)
            curveTo(2.5f, 19f, 3.5f, 19.5f, 5f, 19.5f)
            horizontalLineTo(20f)
            curveTo(21f, 19.5f, 21.5f, 18.5f, 21.5f, 17.5f)
            verticalLineTo(13.5f)
            close()
        }.build()
    }

    val Dress: ImageVector by lazy {
        ImageVector.Builder(
            name = "Dress",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).materialPath {
            moveTo(15f, 3f)
            lineTo(13.5f, 3f)
            curveTo(13.5f, 4.5f, 12.8f, 5.5f, 12f, 5.5f)
            curveTo(11.2f, 5.5f, 10.5f, 4.5f, 10.5f, 3f)
            lineTo(9f, 3f)
            lineTo(7.5f, 9f)
            lineTo(8.5f, 10f)
            lineTo(4f, 21f)
            horizontalLineTo(20f)
            lineTo(15.5f, 10f)
            lineTo(16.5f, 9f)
            close()
        }.build()
    }

    val Skirt: ImageVector by lazy {
        ImageVector.Builder(
            name = "Skirt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).materialPath {
            moveTo(8f, 6f)
            horizontalLineTo(16f)
            lineTo(20f, 19f)
            curveTo(16f, 20.5f, 8f, 20.5f, 4f, 19f)
            close()
        }.build()
    }

    val Tie: ImageVector by lazy {
        ImageVector.Builder(
            name = "Tie",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).materialPath {
            moveTo(10f, 3f)
            horizontalLineTo(14f)
            lineTo(13f, 6f)
            lineTo(14.5f, 17f)
            lineTo(12f, 21f)
            lineTo(9.5f, 17f)
            lineTo(11f, 6f)
            close()
        }.build()
    }

    val Jacket: ImageVector by lazy {
        ImageVector.Builder(
            name = "Jacket",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).materialPath {
            moveTo(16f, 4f)
            lineTo(13.5f, 4f)
            lineTo(12f, 9f)
            lineTo(10.5f, 4f)
            lineTo(8f, 4f)
            lineTo(3f, 7.5f)
            lineTo(5.5f, 11f)
            lineTo(7.5f, 9.5f)
            lineTo(7.5f, 20f)
            lineTo(16.5f, 20f)
            lineTo(16.5f, 9.5f)
            lineTo(18.5f, 11f)
            lineTo(21f, 7.5f)
            close()
        }.build()
    }
}

enum class CategoryFilter(val displayName: String, val icon: ImageVector) {
    ALL("Todas", Icons.Default.GridView),
    SHIRTS("Camisas", CustomIcons.Shirt),
    PANTS("Pantalones", CustomIcons.Pants),
    SHOES("Zapatos", CustomIcons.Shoes),
    TIES("Corbatas", CustomIcons.Tie),
    WATCHES("Relojes", Icons.Default.Watch),
    JACKETS("Chaquetas", CustomIcons.Jacket),
    FRAGRANCES("Fragancias", Icons.Default.AutoAwesome),
    BAGS("Bolsos", Icons.Default.ShoppingBag),
    DRESSES("Vestidos", CustomIcons.Dress),
    SKIRTS("Faldas", CustomIcons.Skirt),
    LAUNDRY("Lavandería", Icons.Default.LocalLaundryService)
}
