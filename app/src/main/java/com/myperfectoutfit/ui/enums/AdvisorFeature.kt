package com.myperfectoutfit.ui.enums

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.ui.graphics.vector.ImageVector

enum class AdvisorFeature(val title: String, val description: String, val icon: ImageVector) {
    DAILY_OUTFIT(
        "Recomendación Diaria",
        "Genera tu outfit ideal basado en tu armario y el clima.",
        Icons.Default.AutoAwesome
    ),
    SUITCASE_IA(
        "Maleta IA",
        "Organiza tu equipaje de forma inteligente para tus viajes.",
        Icons.Default.Luggage
    ),
    FUTURE_PLANNER(
        "Planificador de Outfits",
        "Agenda tus looks para eventos o días futuros.",
        Icons.AutoMirrored.Filled.EventNote
    ),
    CONSULTATIONS(
        "Consultas IA",
        "Resuelve tus dudas sobre moda, estilo y combinaciones.",
        Icons.Default.QuestionAnswer
    )
}
