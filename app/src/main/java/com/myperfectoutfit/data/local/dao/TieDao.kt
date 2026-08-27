package com.myperfectoutfit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myperfectoutfit.data.local.entities.TieEntity
import com.myperfectoutfit.data.local.enums.LaundryState
import kotlinx.coroutines.flow.Flow

@Dao
interface TieDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTie(tie: TieEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTies(ties: List<TieEntity>)

    // Obtiene únicamente las corbatas limpias y disponibles
    @Query("SELECT * FROM `ties` WHERE user_id = :userId AND laundryState = 'CLEAN' ORDER BY code ASC")
    fun getCleanTies(userId: Long): Flow<List<TieEntity>>

    @Query("SELECT * FROM `ties` WHERE user_id = :userId ORDER BY code ASC")
    fun getAllTies(userId: Long): Flow<List<TieEntity>>

    @Query("UPDATE `ties` SET laundryState = :state WHERE id = :tieId")
    suspend fun updateLaundryState(tieId: Long, state: LaundryState)

    @Update
    suspend fun updateTie(tie: TieEntity)

    @Delete
    suspend fun deleteTie(tie: TieEntity)
}