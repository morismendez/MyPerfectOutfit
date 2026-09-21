package com.myperfectoutfit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "suitcases",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SuitcaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long = 1,
    val tripName: String,
    val startDate: String, // yyyy-MM-dd
    val endDate: String,   // yyyy-MM-dd
    val suitcaseType: String, // CARRY_ON, CHECKED
    val tripStyle: String,    // BUSINESS, CASUAL, BEACH, COLD
    val specialEvents: String, // Comma separated: GYM, FORMAL_DINNER...
    val garmentIds: String,    // Comma separated ids: shirt_1, pant_2...
    val missingSuggestions: String? = null, // Comma separated
    val essentials: String? = null,        // Comma separated
    val aiSuggestions: String? = null, // Justificación de la IA
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
