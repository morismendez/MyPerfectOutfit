package com.myperfectoutfit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myperfectoutfit.data.local.entities.BagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBag(bag: BagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBags(bags: List<BagEntity>)

    @Query("SELECT * FROM bags WHERE user_id = :userId AND isAvailable = 1 ORDER BY code ASC")
    fun getAvailableBags(userId: Long): Flow<List<BagEntity>>

    @Query("SELECT * FROM bags WHERE user_id = :userId ORDER BY code ASC")
    fun getAllBags(userId: Long): Flow<List<BagEntity>>

    @Query("UPDATE bags SET isAvailable = :isAvailable WHERE id = :bagId")
    suspend fun updateAvailability(bagId: Long, isAvailable: Boolean)

    @Update
    suspend fun updateBag(bag: BagEntity)

    @Delete
    suspend fun deleteBag(bag: BagEntity)
}