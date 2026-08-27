package com.myperfectoutfit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.myperfectoutfit.data.local.enums.LaundryState

@Entity(
    tableName = "skirts",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SkirtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long = 1,
    val code: String,
    val brand: String? = null,
    val color: String,
    val pattern: String,           // Ej: "Liso", "Cuadros"
    val length: String,            // Ej: "Corta", "Larga"
    val style: String,             // Ej: "Lápiz", "Plisada", "A-line"
    val material: String,
    val imageUrl: String,
    val laundryState: LaundryState = LaundryState.CLEAN
)