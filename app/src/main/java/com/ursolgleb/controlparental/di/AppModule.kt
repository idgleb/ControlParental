package com.ursolgleb.controlparental.di

import android.content.Context
import com.ursolgleb.controlparental.AppDataRepository
import com.ursolgleb.controlparental.data.local.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideAppDataRepository(
        appDatabase: AppDatabase,
        @ApplicationContext context: Context
    ): AppDataRepository {
        return AppDataRepository(appDatabase, context)
    }
}
