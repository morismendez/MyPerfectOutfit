package com.myperfectoutfit.data.repository

import com.myperfectoutfit.data.local.dao.PlannedOutfitWithDetails
import com.myperfectoutfit.data.local.entities.PlannedOutfitEntity
import kotlinx.coroutines.flow.Flow

interface PlanningRepository {
    fun getPlannedOutfitsInRange(userId: Long, startDate: String, endDate: String): Flow<List<PlannedOutfitWithDetails>>
    fun getPlannedOutfitByDate(userId: Long, date: String): Flow<PlannedOutfitWithDetails?>
    suspend fun savePlannedOutfit(plannedOutfit: PlannedOutfitEntity): Long
    suspend fun deletePlannedOutfit(plannedOutfit: PlannedOutfitEntity)
    suspend fun deletePlannedOutfitByDate(userId: Long, date: String)
}
