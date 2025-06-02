package com.ursolgleb.controlparental.di

import android.content.Context
import androidx.room.Room
import com.ursolgleb.controlparental.data.apps.AppDatabase
import com.ursolgleb.controlparental.data.apps.dao.AppDao
import com.ursolgleb.controlparental.data.apps.dao.HorarioDao
import com.ursolgleb.controlparental.data.apps.dao.AppHorarioDao
import com.ursolgleb.controlparental.data.apps.dao.UsageEventDao
import com.ursolgleb.controlparental.data.apps.dao.UsageStatsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "app_database.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Singleton
    @Provides
    fun provideAppDao(db: AppDatabase): AppDao =
        db.appDao()

    @Singleton
    @Provides
    fun provideHorarioDao(db: AppDatabase): HorarioDao =
        db.horarioDao()

    @Singleton
    @Provides
    fun provideAppHorarioDao(db: AppDatabase): AppHorarioDao =
        db.appHorarioDao()


    @Singleton
    @Provides
    fun provideUsageEventDao(db: AppDatabase): UsageEventDao =
        db.usageEventDao()

    @Singleton
    @Provides
    fun provideUsageStatsDao(db: AppDatabase): UsageStatsDao =
        db.usageStatsDao()

}
