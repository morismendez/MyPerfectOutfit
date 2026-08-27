package com.myperfectoutfit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.myperfectoutfit.data.local.enums.LaundryState

@Entity(
    tableName = "dresses",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DressEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long = 1,
    val code: String,
    val brand: String? = null,
    val color: String,
    val pattern: String,           // Ej: "Floral", "Liso", "Lunares"
    val length: String,            // Ej: "Mini", "Midi", "Maxi"
    val sleeveStyle: String,       // Ej: "Sin mangas", "Corta", "Larga"
    val material: String,
    val imageUrl: String,
    val laundryState: LaundryState = LaundryState.CLEAN
)