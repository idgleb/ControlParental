package com.ursolgleb.controlparental.di

import com.ursolgleb.controlparental.data.remote.RemoteDataRepository
import com.ursolgleb.controlparental.data.remote.api.LaravelApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemoteModule {

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://example.com/api/")
        .client(OkHttpClient.Builder().build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideLaravelApi(retrofit: Retrofit): LaravelApi =
        retrofit.create(LaravelApi::class.java)

    @Provides
    @Singleton
    fun provideRemoteRepository(api: LaravelApi): RemoteDataRepository =
        RemoteDataRepository(api)
}