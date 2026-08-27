package com.myperfectoutfit.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.myperfectoutfit.data.local.entities.OutfitHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutfitHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: OutfitHistoryEntity)

    @Query("SELECT * FROM outfit_history WHERE userId = :userId ORDER BY id DESC LIMIT 10")
    fun getRecentHistory(userId: Long): Flow<List<OutfitHistoryEntity>>
}
