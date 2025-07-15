package com.ursolgleb.controlparental.di

import android.content.Context
import android.content.SharedPreferences
import com.ursolgleb.controlparental.data.local.DeviceRegistrationStatusLocalDataSource
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences =
        context.getSharedPreferences("prefs", Context.MODE_PRIVATE)

    @Provides
    @Singleton
    fun provideDeviceRegistrationStatusLocalDataSource(sharedPreferences: SharedPreferences): DeviceRegistrationStatusLocalDataSource =
        DeviceRegistrationStatusLocalDataSource(sharedPreferences)

/*    fun provideAppDataRepository(
        appDatabase: AppDatabase,
        @ApplicationContext context: Context
    ): AppDataRepository {
        return AppDataRepository(appDatabase, context)
    }*/

}


