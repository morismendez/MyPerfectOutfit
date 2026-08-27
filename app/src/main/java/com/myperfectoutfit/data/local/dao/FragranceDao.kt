package com.myperfectoutfit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myperfectoutfit.data.local.entities.FragranceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FragranceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFragrance(fragrance: FragranceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFragrances(fragrances: List<FragranceEntity>)

    // Obtiene las fragancias disponibles
    @Query("SELECT * FROM fragrances WHERE user_id = :userId AND isAvailable = 1 ORDER BY code ASC")
    fun getAvailableFragrances(userId: Long): Flow<List<FragranceEntity>>

    @Query("SELECT * FROM fragrances WHERE user_id = :userId ORDER BY code ASC")
    fun getAllFragrances(userId: Long): Flow<List<FragranceEntity>>

    @Query("UPDATE fragrances SET isAvailable = :isAvailable WHERE id = :fragranceId")
    suspend fun updateAvailability(fragranceId: Long, isAvailable: Boolean)

    @Update
    suspend fun updateFragrance(fragrance: FragranceEntity)

    @Delete
    suspend fun deleteFragrance(fragrance: FragranceEntity)
}