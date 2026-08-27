package com.myperfectoutfit.data.local.converters

import androidx.room.TypeConverter
import com.myperfectoutfit.data.local.enums.LaundryState

class RoomConverters {
    @TypeConverter
    fun fromLaundryState(state: LaundryState): String = state.name

    @TypeConverter
    fun toLaundryState(value: String): LaundryState = LaundryState.valueOf(value)
}
