package com.myperfectoutfit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myperfectoutfit.data.local.entities.JacketEntity
import com.myperfectoutfit.data.local.enums.LaundryState
import kotlinx.coroutines.flow.Flow

@Dao
interface JacketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJacket(jacket: JacketEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJackets(jackets: List<JacketEntity>)

    @Query("SELECT * FROM jackets WHERE user_id = :userId AND laundryState = 'CLEAN' ORDER BY code ASC")
    fun getCleanJackets(userId: Long): Flow<List<JacketEntity>>

    @Query("SELECT * FROM jackets WHERE user_id = :userId ORDER BY code ASC")
    fun getAllJackets(userId: Long): Flow<List<JacketEntity>>

    @Query("UPDATE jackets SET laundryState = :state WHERE id = :jacketId")
    suspend fun updateLaundryState(jacketId: Long, state: LaundryState)

    @Update
    suspend fun updateJacket(jacket: JacketEntity)

    @Delete
    suspend fun deleteJacket(jacket: JacketEntity)
}