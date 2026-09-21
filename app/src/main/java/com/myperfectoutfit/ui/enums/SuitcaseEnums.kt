package com.myperfectoutfit.ui.enums

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class SuitcaseType(val title: String, val icon: ImageVector) {
    CARRY_ON("Equipaje de mano", Icons.Default.BusinessCenter),
    CHECKED("Maleta documentada", Icons.Default.Luggage)
}

enum class TripStyle(val title: String, val icon: ImageVector) {
    BUSINESS("Negocios / Formal", Icons.Default.Work),
    CASUAL("Casual / Urbano", Icons.Default.DirectionsWalk),
    BEACH("Playa / Cálido", Icons.Default.WbSunny),
    COLD("Clima Frío", Icons.Default.AcUnit)
}

enum class SpecialEvent(val title: String) {
    FORMAL_DINNER("Cena formal de noche"),
    GYM("Ejercicio / Gimnasio"),
    OUTDOOR("Caminata al aire libre"),
    OTHER("Otro (especificar)")
}
