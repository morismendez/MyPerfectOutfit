package com.myperfectoutfit.di

import android.content.Context
import com.myperfectoutfit.data.local.AppDatabase
import com.myperfectoutfit.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideUserDao(db: AppDatabase): UserDao = db.userDao()

    @Provides
    fun provideShirtDao(db: AppDatabase): ShirtDao = db.shirtDao()

    @Provides
    fun providePantDao(db: AppDatabase): PantDao = db.pantDao()

    @Provides
    fun provideTieDao(db: AppDatabase): TieDao = db.tieDao()

    @Provides
    fun provideShoeDao(db: AppDatabase): ShoeDao = db.shoeDao()

    @Provides
    fun provideWatchDao(db: AppDatabase): WatchDao = db.watchDao()

    @Provides
    fun provideFragranceDao(db: AppDatabase): FragranceDao = db.fragranceDao()

    @Provides
    fun provideJacketDao(db: AppDatabase): JacketDao = db.jacketDao()

    @Provides
    fun provideOutfitDao(db: AppDatabase): OutfitDao = db.outfitDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase): OutfitHistoryDao = db.historyDao()
}
