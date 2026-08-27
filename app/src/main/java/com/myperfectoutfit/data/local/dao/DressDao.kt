package com.myperfectoutfit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myperfectoutfit.data.local.entities.DressEntity
import com.myperfectoutfit.data.local.enums.LaundryState
import kotlinx.coroutines.flow.Flow

@Dao
interface DressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDress(dress: DressEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDresses(dresses: List<DressEntity>)

    @Query("SELECT * FROM dresses WHERE user_id = :userId AND laundryState = 'CLEAN' ORDER BY code ASC")
    fun getCleanDresses(userId: Long): Flow<List<DressEntity>>

    @Query("SELECT * FROM dresses WHERE user_id = :userId ORDER BY code ASC")
    fun getAllDresses(userId: Long): Flow<List<DressEntity>>

    @Query("UPDATE dresses SET laundryState = :state WHERE id = :dressId")
    suspend fun updateLaundryState(dressId: Long, state: LaundryState)

    @Update
    suspend fun updateDress(dress: DressEntity)

    @Delete
    suspend fun deleteDress(dress: DressEntity)
}