package com.myperfectoutfit.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DryCleaning
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Wardrobe : Screen("wardrobe", "Armario", Icons.Default.Checkroom)
    object AiAdvisor : Screen("ai_advisor", "Asesor IA", Icons.Default.AutoAwesome)
    object Laundry : Screen("laundry", "Lavandería", Icons.Default.DryCleaning)
    object Registration : Screen("registration", "Registro", Icons.Default.PersonAdd)
}