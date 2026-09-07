package com.myperfectoutfit.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

import com.myperfectoutfit.data.local.enums.CategoryDisplayMode

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val profilePictureUrl: String? = null,
    val activeCategories: String = "SHIRTS,PANTS,SHOES,JACKETS,FRAGRANCES", // Por defecto algunas básicas
    val categoryDisplayMode: CategoryDisplayMode = CategoryDisplayMode.BOTH,
    val createdAt: Long = System.currentTimeMillis()
)
