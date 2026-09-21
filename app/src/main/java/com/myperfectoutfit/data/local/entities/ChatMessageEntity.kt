package com.myperfectoutfit.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "user_id") val userId: Long,
    val text: String,
    val isUser: Boolean,
    val imageUri: String? = null,
    val suggestedGarmentIds: String? = null, // IDs separados por coma: SHIRT_1, PANT_2...
    val timestamp: Long = System.currentTimeMillis()
)
