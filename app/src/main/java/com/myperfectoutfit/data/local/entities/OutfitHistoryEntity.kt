package com.myperfectoutfit.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "outfit_history")
data class OutfitHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long = 1L,
    val dateString: String, // Ejemplo: "2026-08-18"
    val shirtId: Long?,
    val pantId: Long?,
    val shoeId: Long?,
    val tieId: Long?,
    val watchId: Long?,
    val fragranceId: Long?,
    val jacketId: Long?,
    val bagId: Long?,
    val dressId: Long?,
    val skirtId: Long?,
    val customGarmentIds: String?, // Lista de IDs separados por comas
    val summaryText: String
)
