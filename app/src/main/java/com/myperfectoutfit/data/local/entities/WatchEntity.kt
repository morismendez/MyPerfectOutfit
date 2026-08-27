package com.myperfectoutfit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "watches",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long = 1,
    val code: String,              // Ej: "R01"
    val brand: String,             // Ej: "Orient", "Pagani Design", "Seiko"
    val model: String,             // Ej: "Bambino"
    val dialColor: String,         // Ej: "Negra"
    val strapColor: String,        // Ej: "Negra"
    val strapMaterial: String,     // Ej: "Cuero", "Acero"
    val imageUrl: String,
    val isAvailable: Boolean = true
)