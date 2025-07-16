package com.ursolgleb.controlparental.di

import com.ursolgleb.controlparental.data.auth.remote.DeviceAuthApiService
import com.ursolgleb.controlparental.data.auth.interceptor.DeviceAuthInterceptor
import com.ursolgleb.controlparental.data.auth.repository.DeviceAuthRepositoryImpl
import com.ursolgleb.controlparental.domain.auth.repository.DeviceAuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Módulo Hilt para autenticación
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    
    @Binds
    abstract fun bindDeviceAuthRepository(
        impl: DeviceAuthRepositoryImpl
    ): DeviceAuthRepository
    
    companion object {
        
        @Provides
        @Singleton
        @Named("authenticated")
        fun provideAuthenticatedRetrofit(
            @Named("base") baseRetrofit: Retrofit,
            baseOkHttpClient: OkHttpClient,
            authInterceptor: DeviceAuthInterceptor
        ): Retrofit {
            val client = baseOkHttpClient.newBuilder()
                .addInterceptor(authInterceptor)
                .build()
                
            return baseRetrofit.newBuilder()
                .client(client)
                .build()
        }
        

    }
} 