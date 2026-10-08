package com.myperfectoutfit.ui.enums

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.myperfectoutfit.R

enum class SuitcaseType(@StringRes val titleRes: Int, val icon: ImageVector) {
    CARRY_ON(R.string.suitcase_hand_bag, Icons.Default.BusinessCenter),
    CHECKED(R.string.suitcase_large, Icons.Default.Luggage)
}

enum class TripStyle(@StringRes val titleRes: Int, val icon: ImageVector) {
    BUSINESS(R.string.style_business, Icons.Default.Work),
    CASUAL(R.string.style_casual, Icons.Default.DirectionsWalk),
    BEACH(R.string.style_beach, Icons.Default.WbSunny),
    COLD(R.string.style_cold, Icons.Default.AcUnit)
}

enum class SpecialEvent(@StringRes val titleRes: Int) {
    FORMAL_DINNER(R.string.event_formal_dinner),
    GYM(R.string.event_gym),
    OUTDOOR(R.string.event_outdoor),
    OTHER(R.string.event_other)
}
