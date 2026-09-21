package com.myperfectoutfit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "planned_outfits",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlannedOutfitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long = 1,
    val dateString: String, // Formato: yyyy-MM-dd
    val shirtId: Long? = null,
    val pantId: Long? = null,
    val shoeId: Long? = null,
    val tieId: Long? = null,
    val watchId: Long? = null,
    val fragranceId: Long? = null,
    val jacketId: Long? = null,
    val bagId: Long? = null,
    val dressId: Long? = null,
    val skirtId: Long? = null,
    val customGarmentIds: String? = null, // IDs separados por coma
    val notes: String? = null,
    val isConfirmed: Boolean = false
)
