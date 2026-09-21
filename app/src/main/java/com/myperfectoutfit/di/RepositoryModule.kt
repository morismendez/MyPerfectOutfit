package com.myperfectoutfit.di

import com.myperfectoutfit.data.local.AppDatabase
import com.myperfectoutfit.data.local.backup.BackupManager
import com.myperfectoutfit.data.local.security.SecurePreferences
import com.myperfectoutfit.data.repository.PlanningRepository
import com.myperfectoutfit.data.repository.PlanningRepositoryImpl
import com.myperfectoutfit.data.repository.SuitcaseRepository
import com.myperfectoutfit.data.repository.SuitcaseRepositoryImpl
import com.myperfectoutfit.data.repository.WardrobeRepository
import com.myperfectoutfit.data.repository.WardrobeRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideWardrobeRepository(
        db: AppDatabase,
        securePrefs: SecurePreferences,
        backupManager: BackupManager
    ): WardrobeRepository {
        return WardrobeRepositoryImpl(db, securePrefs, backupManager)
    }

    @Provides
    @Singleton
    fun providePlanningRepository(
        db: AppDatabase
    ): PlanningRepository {
        return PlanningRepositoryImpl(db)
    }

    @Provides
    @Singleton
    fun provideSuitcaseRepository(
        db: AppDatabase
    ): SuitcaseRepository {
        return SuitcaseRepositoryImpl(db)
    }
}