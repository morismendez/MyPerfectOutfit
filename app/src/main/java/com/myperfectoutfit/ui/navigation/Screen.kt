package com.myperfectoutfit.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.ui.graphics.vector.ImageVector
import com.myperfectoutfit.R

sealed class Screen(val route: String, @StringRes val titleRes: Int, val icon: ImageVector) {
    object Wardrobe : Screen("wardrobe", R.string.nav_wardrobe, Icons.Default.Checkroom)
    object AiAdvisor : Screen("ai_advisor", R.string.nav_ai_advisor, Icons.Default.AutoAwesome)
    object History : Screen("history", R.string.nav_history, Icons.Default.History)
    object Laundry : Screen("laundry", R.string.nav_laundry, Icons.Default.DryCleaning)
    object Registration : Screen("registration", R.string.nav_wardrobe, Icons.Default.PersonAdd)
}