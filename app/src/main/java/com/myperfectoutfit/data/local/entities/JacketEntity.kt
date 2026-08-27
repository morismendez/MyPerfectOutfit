package com.myperfectoutfit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.myperfectoutfit.data.local.enums.LaundryState

@Entity(
    tableName = "jackets",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class JacketEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long = 1,
    val code: String,              // Ej: "CH01"
    val brand: String? = null,
    val color: String,
    val type: String,              // Ej: "Blazer", "Suéter", "Chaqueta"
    val closureType: String,       // Ej: "Botones", "Cremallera"
    val imageUrl: String,
    val laundryState: LaundryState = LaundryState.CLEAN
)