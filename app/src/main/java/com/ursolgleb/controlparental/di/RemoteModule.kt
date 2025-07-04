package com.ursolgleb.controlparental.di

import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.ursolgleb.controlparental.data.remote.RemoteDataRepository
import com.ursolgleb.controlparental.data.remote.api.LaravelApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RemoteModule {
    
    // CAMBIAR SEGÚN TU CONFIGURACIÓN:
    // - Emulador: "http://10.0.2.2/api/"
    // - Dispositivo físico: "http://192.168.1.35/api/"
    private const val BASE_URL = "https://87f4-200-117-178-44.ngrok-free.app/api/"

    @Provides
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
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }


    @Provides
    fun provideRetrofit(
        moshi: Moshi,
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
    }

    @Provides
    @Singleton
    fun provideLaravelApi(retrofit: Retrofit): LaravelApi =
        retrofit.create(LaravelApi::class.java)

    @Provides
    @Singleton
    fun provideRemoteRepository(api: LaravelApi): RemoteDataRepository =
        RemoteDataRepository(api)
}