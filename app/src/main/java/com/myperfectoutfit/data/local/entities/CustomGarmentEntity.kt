package com.myperfectoutfit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.myperfectoutfit.data.local.enums.LaundryState

@Entity(
    tableName = "custom_garments",
    foreignKeys = [
        ForeignKey(
            entity = CustomCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CustomGarmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "user_id") val userId: Long = 1,
    val imageUrl: String,
    val laundryState: LaundryState = LaundryState.CLEAN,
    val isAvailable: Boolean = true,
    val attributeValues: String // JSON o String mapeado: "Color:Rojo|Material:Lana"
)
