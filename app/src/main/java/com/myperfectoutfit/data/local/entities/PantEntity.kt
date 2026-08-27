package com.myperfectoutfit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.myperfectoutfit.data.local.enums.LaundryState

@Entity(
    tableName = "pants",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long = 1,
    val code: String,
    val brand: String? = null,
    val subType: String = "Pantalón", // Ej: Jeans, Dress Pants, Shorts, Joggers, Jumpsuit
    val primaryColor: String,
    val secondaryColor: String? = null,
    val material: String = "Algodón", // Ej: Denim, Lino, Gabardina, Cuero
    val lengthStyle: String = "Largo", // Ej: Largo, Tobillero, Capri, Corto
    val waistRise: String = "Tiro medio", // Ej: Tiro alto, Tiro medio, Tiro bajo
    val fitStyle: String = "Recto", // Ej: Slim, Skinny, Wide Leg
    val formalityLevel: String = "Casual", // Ej: Formal, Smart Casual, Casual, Deportivo
    val imageUrl: String,
    val laundryState: LaundryState = LaundryState.CLEAN
)