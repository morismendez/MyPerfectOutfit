package com.myperfectoutfit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myperfectoutfit.data.local.entities.SkirtEntity
import com.myperfectoutfit.data.local.enums.LaundryState
import kotlinx.coroutines.flow.Flow

@Dao
interface SkirtDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkirt(skirt: SkirtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSkirts(skirts: List<SkirtEntity>)

    @Query("SELECT * FROM skirts WHERE user_id = :userId AND laundryState = 'CLEAN' ORDER BY code ASC")
    fun getCleanSkirts(userId: Long): Flow<List<SkirtEntity>>

    @Query("SELECT * FROM skirts WHERE user_id = :userId ORDER BY code ASC")
    fun getAllSkirts(userId: Long): Flow<List<SkirtEntity>>

    @Query("UPDATE skirts SET laundryState = :state WHERE id = :skirtId")
    suspend fun updateLaundryState(skirtId: Long, state: LaundryState)

    @Update
    suspend fun updateSkirt(skirt: SkirtEntity)

    @Delete
    suspend fun deleteSkirt(skirt: SkirtEntity)
}