package com.myperfectoutfit.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.myperfectoutfit.data.local.entities.ShirtEntity
import com.myperfectoutfit.data.local.enums.LaundryState
import kotlinx.coroutines.flow.Flow

@Dao
interface ShirtDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShirt(shirt: ShirtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShirts(shirts: List<ShirtEntity>)

    // Filtra únicamente camisas disponibles en el armario del usuario
    @Query("SELECT * FROM shirts WHERE user_id = :userId AND laundryState = 'CLEAN' ORDER BY code ASC")
    fun getCleanShirts(userId: Long): Flow<List<ShirtEntity>>

    // Obtiene el inventario completo (incluyendo lo que está en lavandería)
    @Query("SELECT * FROM shirts WHERE user_id = :userId ORDER BY code ASC")
    fun getAllShirts(userId: Long): Flow<List<ShirtEntity>>

    // Actualiza el estado de lavandería de una camisa específica
    @Query("UPDATE shirts SET laundryState = :state WHERE id = :shirtId")
    suspend fun updateLaundryState(shirtId: Long, state: LaundryState)

    // Marca múltiples camisas como enviadas a la lavandería en un solo comando
    @Query("UPDATE shirts SET laundryState = 'IN_LAUNDRY' WHERE id IN (:shirtIds)")
    suspend fun sendToLaundry(shirtIds: List<Long>)

    @Update
    suspend fun updateShirt(shirt: ShirtEntity)

   @Delete
    suspend fun deleteShirt(shirt: ShirtEntity)
}