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
    private const val BASE_URL = "http://10.0.2.2/api/" // IP de tu máquina en la red

    @Provides
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .add(object : JsonAdapter.Factory {
                override fun create(
                    type: java.lang.reflect.Type,
                    annotations: Set<Annotation>,
                    moshi: Moshi
                ): JsonAdapter<*>? {
                    val delegate = moshi.nextAdapter<Any>(this, type, annotations)
                    return object : JsonAdapter<Any>() {
                        override fun fromJson(reader: JsonReader): Any? {
                            val lenientReader = reader.peekJson().apply {
                                isLenient = true
                            }
                            return delegate.fromJson(lenientReader)
                        }
                        override fun toJson(writer: com.squareup.moshi.JsonWriter, value: Any?) {
                            delegate.toJson(writer, value)
                        }
                    }
                }
            })
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
            .addConverterFactory(MoshiConverterFactory.create(moshi))
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