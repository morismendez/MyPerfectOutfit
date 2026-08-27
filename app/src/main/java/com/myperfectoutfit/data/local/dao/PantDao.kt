package com.myperfectoutfit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myperfectoutfit.data.local.entities.PantEntity
import com.myperfectoutfit.data.local.enums.LaundryState
import kotlinx.coroutines.flow.Flow

@Dao
interface PantDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPant(pant: PantEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPants(pants: List<PantEntity>)

    // Consulta relacional de pantalones limpios
    @Query("SELECT * FROM pants WHERE user_id = :userId AND laundryState = 'CLEAN' ORDER BY code ASC")
    fun getCleanPants(userId: Long): Flow<List<PantEntity>>

    @Query("SELECT * FROM pants WHERE user_id = :userId ORDER BY code ASC")
    fun getAllPants(userId: Long): Flow<List<PantEntity>>

    @Query("UPDATE pants SET laundryState = :state WHERE id = :pantId")
    suspend fun updateLaundryState(pantId: Long, state: LaundryState)

    @Update
    suspend fun updatePant(pant: PantEntity)

    @Delete
    suspend fun deletePant(pant: PantEntity)
}