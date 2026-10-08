package com.myperfectoutfit.ui.enums

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.ui.graphics.vector.ImageVector
import com.myperfectoutfit.R

enum class AdvisorFeature(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val icon: ImageVector
) {
    DAILY_OUTFIT(
        R.string.feature_daily_title,
        R.string.feature_daily_desc,
        Icons.Default.AutoAwesome
    ),
    SUITCASE_IA(
        R.string.feature_suitcase_title,
        R.string.feature_suitcase_desc,
        Icons.Default.Luggage
    ),
    FUTURE_PLANNER(
        R.string.feature_planner_title,
        R.string.feature_planner_desc,
        Icons.AutoMirrored.Filled.EventNote
    ),
    CONSULTATIONS(
        R.string.feature_chat_title,
        R.string.feature_chat_desc,
        Icons.Default.QuestionAnswer
    )
}
