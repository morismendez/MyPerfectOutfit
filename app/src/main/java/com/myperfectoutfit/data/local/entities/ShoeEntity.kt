package com.myperfectoutfit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "shoes",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ShoeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long = 1,
    val code: String,
    val brand: String,
    val subType: String = "Calzado", // Zapatos de vestir, Tenis, Sandalias, Tacones, etc.
    val style: String = "Casual", // Oxford, Derby, Loafer, Chelsea, Stiletto, etc.
    val color: String,
    val secondaryColor: String? = null,
    val material: String = "Cuero", // Cuero, Gamuza, Tela, Sintético
    val heelHeightStyle: String = "Plano", // Plano, Tacón bajo, Tacón alto, Plataforma
    val toeStyle: String = "Puntera lisa", // Cap Toe, En punta, Cuadrada, Peep Toe
    val closureType: String = "Cordones", // Cordones, Hebilla, Slip-on, Cremallera
    val formalityLevel: String = "Casual", // Formal, Smart Casual, Casual, Deportivo
    val imageUrl: String,
    val isAvailable: Boolean = true
)