package com.myperfectoutfit.data.local.converters

import androidx.room.TypeConverter
import com.myperfectoutfit.data.local.enums.LaundryState

class Converters {

    @TypeConverter
    fun fromLaundryState(value: LaundryState?): String? {
        return value?.name
    }

    @TypeConverter
    fun toLaundryState(value: String?): LaundryState? {
        return value?.let {
            try {
                enumValueOf<LaundryState>(it)
            } catch (e: IllegalArgumentException) {
                LaundryState.CLEAN
            }
        }
    }
}