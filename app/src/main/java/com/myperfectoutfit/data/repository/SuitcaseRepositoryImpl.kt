package com.myperfectoutfit.data.repository

import com.myperfectoutfit.data.local.AppDatabase
import com.myperfectoutfit.data.local.entities.SuitcaseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SuitcaseRepositoryImpl(
    private val db: AppDatabase
) : SuitcaseRepository {

    private val suitcaseDao = db.suitcaseDao()

    override fun getAllSuitcases(userId: Long): Flow<List<SuitcaseEntity>> {
        return suitcaseDao.getAllSuitcases(userId)
    }

    override suspend fun saveSuitcase(suitcase: SuitcaseEntity): Long = withContext(Dispatchers.IO) {
        suitcaseDao.insertSuitcase(suitcase)
    }

    override suspend fun deleteSuitcase(suitcase: SuitcaseEntity) = withContext(Dispatchers.IO) {
        suitcaseDao.deleteSuitcase(suitcase)
    }

    override suspend fun getSuitcaseById(id: Long): SuitcaseEntity? = withContext(Dispatchers.IO) {
        suitcaseDao.getSuitcaseById(id)
    }

    override fun getActiveSuitcase(userId: Long, date: String): Flow<SuitcaseEntity?> {
        return suitcaseDao.getActiveSuitcase(userId, date)
    }
}
