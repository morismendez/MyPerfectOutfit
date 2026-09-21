package com.myperfectoutfit.data.repository

import com.myperfectoutfit.data.local.AppDatabase
import com.myperfectoutfit.data.local.dao.PlannedOutfitWithDetails
import com.myperfectoutfit.data.local.entities.PlannedOutfitEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PlanningRepositoryImpl(
    private val db: AppDatabase
) : PlanningRepository {

    private val plannedOutfitDao = db.plannedOutfitDao()

    override fun getPlannedOutfitsInRange(userId: Long, startDate: String, endDate: String): Flow<List<PlannedOutfitWithDetails>> {
        return plannedOutfitDao.getPlannedOutfitsInRange(userId, startDate, endDate)
    }

    override fun getPlannedOutfitByDate(userId: Long, date: String): Flow<PlannedOutfitWithDetails?> {
        return plannedOutfitDao.getPlannedOutfitByDate(userId, date)
    }

    override suspend fun savePlannedOutfit(plannedOutfit: PlannedOutfitEntity): Long = withContext(Dispatchers.IO) {
        plannedOutfitDao.insertPlannedOutfit(plannedOutfit)
    }

    override suspend fun deletePlannedOutfit(plannedOutfit: PlannedOutfitEntity) = withContext(Dispatchers.IO) {
        plannedOutfitDao.deletePlannedOutfit(plannedOutfit)
    }

    override suspend fun deletePlannedOutfitByDate(userId: Long, date: String) = withContext(Dispatchers.IO) {
        plannedOutfitDao.deletePlannedOutfitByDate(userId, date)
    }
}
