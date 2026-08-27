package com.myperfectoutfit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myperfectoutfit.data.local.entities.WatchEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatch(watch: WatchEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatches(watches: List<WatchEntity>)

    // Obtiene los relojes disponibles
    @Query("SELECT * FROM watches WHERE user_id = :userId AND isAvailable = 1 ORDER BY code ASC")
    fun getAvailableWatches(userId: Long): Flow<List<WatchEntity>>

    @Query("SELECT * FROM watches WHERE user_id = :userId ORDER BY code ASC")
    fun getAllWatches(userId: Long): Flow<List<WatchEntity>>

    @Query("UPDATE watches SET isAvailable = :isAvailable WHERE id = :watchId")
    suspend fun updateAvailability(watchId: Long, isAvailable: Boolean)

    @Update
    suspend fun updateWatch(watch: WatchEntity)

    @Delete
    suspend fun deleteWatch(watch: WatchEntity)
}
