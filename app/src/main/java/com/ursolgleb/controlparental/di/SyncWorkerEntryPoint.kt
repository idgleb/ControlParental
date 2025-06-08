package com.ursolgleb.controlparental.di

import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.data.remote.RemoteDataRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun getAppDataRepository(): AppDataRepository
    fun getRemoteDataRepository(): RemoteDataRepository
}