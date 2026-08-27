package com.myperfectoutfit.data.local.dao

import androidx.room.*
import com.myperfectoutfit.data.local.entities.CustomCategoryEntity
import com.myperfectoutfit.data.local.entities.CustomGarmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CustomCategoryEntity): Long

    @Query("SELECT * FROM custom_categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: Long): CustomCategoryEntity?

    @Query("SELECT * FROM custom_categories WHERE user_id = :userId ORDER BY name ASC")
    fun getCategories(userId: Long): Flow<List<CustomCategoryEntity>>

    @Update
    suspend fun updateCategory(category: CustomCategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CustomCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGarment(garment: CustomGarmentEntity): Long

    @Query("SELECT * FROM custom_garments WHERE category_id = :categoryId")
    fun getGarmentsByCategory(categoryId: Long): Flow<List<CustomGarmentEntity>>

    @Query("SELECT * FROM custom_garments WHERE user_id = :userId")
    fun getAllCustomGarments(userId: Long): Flow<List<CustomGarmentEntity>>

    @Update
    suspend fun updateGarment(garment: CustomGarmentEntity)

    @Delete
    suspend fun deleteGarment(garment: CustomGarmentEntity)
    
    @Query("UPDATE custom_garments SET laundryState = :state WHERE id = :garmentId")
    suspend fun updateLaundryState(garmentId: Long, state: String)

    @Query("UPDATE custom_garments SET isAvailable = :isAvailable WHERE id = :garmentId")
    suspend fun updateAvailability(garmentId: Long, isAvailable: Boolean)
}
