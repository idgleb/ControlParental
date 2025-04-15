package com.ursolgleb.controlparental.di

import android.content.Context
import androidx.room.Room
import com.ursolgleb.controlparental.data.local.AppDatabase
import com.ursolgleb.controlparental.data.log.LogAppBlockerDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LogDatabaseModule {
    @Singleton
    @Provides
    fun provideLogAppBlockerDatabase(@ApplicationContext appContext: Context): LogAppBlockerDatabase {
        return Room.databaseBuilder(
            appContext,
            LogAppBlockerDatabase::class.java,
            "app_blocker.db"
        ).fallbackToDestructiveMigration().build()

    }
}
