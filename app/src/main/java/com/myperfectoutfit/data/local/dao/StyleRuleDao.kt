package com.myperfectoutfit.data.local.dao

import androidx.room.*
import com.myperfectoutfit.data.local.entities.StyleRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StyleRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: StyleRuleEntity): Long

    @Query("SELECT * FROM style_rules WHERE user_id = :userId ORDER BY id DESC")
    fun getRules(userId: Long): Flow<List<StyleRuleEntity>>

    @Query("SELECT * FROM style_rules WHERE user_id = :userId AND isActive = 1")
    fun getActiveRules(userId: Long): Flow<List<StyleRuleEntity>>

    @Update
    suspend fun updateRule(rule: StyleRuleEntity)

    @Delete
    suspend fun deleteRule(rule: StyleRuleEntity)
    
    @Query("UPDATE style_rules SET isActive = :isActive WHERE id = :ruleId")
    suspend fun toggleRule(ruleId: Long, isActive: Boolean)
}
