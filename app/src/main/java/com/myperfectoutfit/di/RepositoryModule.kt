package com.myperfectoutfit.di

import com.myperfectoutfit.data.local.AppDatabase
import com.myperfectoutfit.data.local.backup.BackupManager
import com.myperfectoutfit.data.local.security.SecurePreferences
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
}