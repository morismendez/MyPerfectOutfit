package com.myperfectoutfit.data.local.dao

import androidx.room.*
import com.myperfectoutfit.data.local.entities.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("SELECT * FROM chat_messages WHERE user_id = :userId ORDER BY timestamp ASC")
    fun getChatHistory(userId: Long): Flow<List<ChatMessageEntity>>

    @Query("DELETE FROM chat_messages WHERE user_id = :userId")
    suspend fun clearHistory(userId: Long)

    @Query("DELETE FROM chat_messages WHERE id NOT IN (SELECT id FROM chat_messages WHERE user_id = :userId ORDER BY timestamp DESC LIMIT 50)")
    suspend fun limitHistory(userId: Long)
}
