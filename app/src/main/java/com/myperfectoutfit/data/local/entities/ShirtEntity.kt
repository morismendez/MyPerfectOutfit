package com.myperfectoutfit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.myperfectoutfit.data.local.enums.LaundryState

@Entity(
    tableName = "shirts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ShirtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long = 1,
    val code: String,
    val brand: String,
    val subType: String = "Camisa", // Ej: T-Shirt, Polo, Blusa, Top
    val primaryColor: String,
    val secondaryColor: String? = null,
    val pattern: String,
    val sleeveLength: String = "Manga larga", // Ej: Corta, 3/4, Sin mangas
    val necklineStyle: String = "Camisero", // Ej: Cuello redondo, Mao, Escote V
    val material: String = "Algodón",
    val formalityLevel: String = "Casual", // Ej: Formal, Smart Casual
    val fit: String = "Regular",
    val imageUrl: String,
    val laundryState: LaundryState = LaundryState.CLEAN
)