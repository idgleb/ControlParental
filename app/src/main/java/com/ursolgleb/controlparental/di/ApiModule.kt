package com.ursolgleb.controlparental.di

import com.ursolgleb.controlparental.data.auth.remote.DeviceAuthApiService
import com.ursolgleb.controlparental.data.remote.RemoteDataRepository
import com.ursolgleb.controlparental.data.remote.api.LaravelApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Módulo Hilt para servicios API
 */
@Module
@InstallIn(SingletonComponent::class)
object ApiModule {
    
    @Provides
    @Singleton
    fun provideLaravelApi(
        @Named("base") retrofit: Retrofit
    ): LaravelApi {
        // El retrofit base ya tiene el OkHttpClient con DeviceAuthInterceptor configurado
        return retrofit.create(LaravelApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideDeviceAuthApiService(
        @Named("base") retrofit: Retrofit
    ): DeviceAuthApiService {
        return retrofit.create(DeviceAuthApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideRemoteDataRepository(
        api: LaravelApi
    ): RemoteDataRepository {
        return RemoteDataRepository(api)
    }
} 