package com.myperfectoutfit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myperfectoutfit.data.local.entities.ShoeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoe(shoe: ShoeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoes(shoes: List<ShoeEntity>)

    // Obtiene únicamente calzado disponible para uso (no en reparación o mantenimiento)
    @Query("SELECT * FROM shoes WHERE user_id = :userId AND isAvailable = 1 ORDER BY code ASC")
    fun getAvailableShoes(userId: Long): Flow<List<ShoeEntity>>

    @Query("SELECT * FROM shoes WHERE user_id = :userId ORDER BY code ASC")
    fun getAllShoes(userId: Long): Flow<List<ShoeEntity>>

    @Query("UPDATE shoes SET isAvailable = :isAvailable WHERE id = :shoeId")
    suspend fun updateAvailability(shoeId: Long, isAvailable: Boolean)

    @Update
    suspend fun updateShoe(shoe: ShoeEntity)

    @Delete
    suspend fun deleteShoe(shoe: ShoeEntity)
}