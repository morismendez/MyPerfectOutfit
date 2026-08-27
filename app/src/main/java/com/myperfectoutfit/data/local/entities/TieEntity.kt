package com.myperfectoutfit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.myperfectoutfit.data.local.enums.LaundryState

@Entity(
    tableName = "ties",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TieEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long = 1,
    val code: String,              // Ej: "CO01"
    val colorRange: String,        // Ej: "Gama de Azules", "Burdeos"
    val pattern: String,           // Ej: "Diagonal", "Micro-patrón"
    val material: String = "Seda",
    val widthCms: Double = 7.5,
    val imageUrl: String,
    val laundryState: LaundryState = LaundryState.CLEAN
)