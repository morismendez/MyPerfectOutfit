package com.myperfectoutfit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "fragrances",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FragranceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long = 1,
    val code: String,              // Ej: "F01"
    val brand: String,             // Ej: "Giorgio Armani"
    val name: String,              // Ej: "Acqua di Giò Profondo"
    val occasionTag: String,       // Ej: "Oficina / Diario", "Calor"
    val profile: String,           // Ej: "Fresco / Marino", "Amaderado"
    val imageUrl: String,
    val isAvailable: Boolean = true
)