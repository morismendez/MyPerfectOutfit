package com.myperfectoutfit.data.repository

import com.myperfectoutfit.data.local.entities.SuitcaseEntity
import kotlinx.coroutines.flow.Flow

interface SuitcaseRepository {
    fun getAllSuitcases(userId: Long): Flow<List<SuitcaseEntity>>
    suspend fun saveSuitcase(suitcase: SuitcaseEntity): Long
    suspend fun deleteSuitcase(suitcase: SuitcaseEntity)
    suspend fun getSuitcaseById(id: Long): SuitcaseEntity?
    fun getActiveSuitcase(userId: Long, date: String): Flow<SuitcaseEntity?>
}
