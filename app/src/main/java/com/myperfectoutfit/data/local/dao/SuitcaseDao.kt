package com.myperfectoutfit.data.local.dao

import androidx.room.*
import com.myperfectoutfit.data.local.entities.SuitcaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SuitcaseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuitcase(suitcase: SuitcaseEntity): Long

    @Update
    suspend fun updateSuitcase(suitcase: SuitcaseEntity)

    @Delete
    suspend fun deleteSuitcase(suitcase: SuitcaseEntity)

    @Query("SELECT * FROM suitcases WHERE user_id = :userId ORDER BY createdAt DESC")
    fun getAllSuitcases(userId: Long): Flow<List<SuitcaseEntity>>

    @Query("SELECT * FROM suitcases WHERE id = :id LIMIT 1")
    suspend fun getSuitcaseById(id: Long): SuitcaseEntity?

    @Query("SELECT * FROM suitcases WHERE user_id = :userId AND startDate <= :date AND endDate >= :date AND isCompleted = 0 LIMIT 1")
    fun getActiveSuitcase(userId: Long, date: String): Flow<SuitcaseEntity?>
}
