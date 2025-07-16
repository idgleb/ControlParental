package com.ursolgleb.controlparental.di

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ursolgleb.controlparental.BuildConfig
import com.ursolgleb.controlparental.data.auth.interceptor.DeviceAuthInterceptor
import com.ursolgleb.controlparental.interceptors.DeviceDeletedInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Módulo Hilt para configuración de red
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(object : JsonAdapter.Factory {
                override fun create(
                    type: java.lang.reflect.Type,
                    annotations: Set<Annotation>,
                    moshi: Moshi
                ): JsonAdapter<*>? {
                    if (type == Long::class.java || type == java.lang.Long::class.java) {
                        return object : JsonAdapter<Long>() {
                            override fun fromJson(reader: JsonReader): Long? {
                                return if (reader.peek() == JsonReader.Token.NUMBER) {
                                    reader.nextLong()
                                } else {
                                    reader.skipValue()
                                    null
                                }
                            }
                            override fun toJson(writer: com.squareup.moshi.JsonWriter, value: Long?) {
                                writer.value(value)
                            }
                        }
                    }
                    return null
                }
            })
            .add(KotlinJsonAdapterFactory())
            .build()
    }
    
    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
    }
    
    @Provides
    @Singleton
    fun provideBaseOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        deviceAuthInterceptor: DeviceAuthInterceptor,
        deviceDeletedInterceptor: DeviceDeletedInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(deviceAuthInterceptor)
            .addInterceptor(deviceDeletedInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    @Provides
    @Singleton
    @Named("base")
    fun provideBaseRetrofit(
        moshi: Moshi,
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
    }
} 