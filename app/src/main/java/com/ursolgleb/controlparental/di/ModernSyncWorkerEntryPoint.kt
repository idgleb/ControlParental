package com.ursolgleb.controlparental.di

import com.ursolgleb.controlparental.data.local.AppDataRepository
import com.ursolgleb.controlparental.data.remote.RemoteDataRepository
import com.ursolgleb.controlparental.handlers.EventSyncManager
import com.ursolgleb.controlparental.handlers.SyncHandler
import com.ursolgleb.controlparental.data.auth.local.DeviceAuthLocalDataSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ModernSyncWorkerEntryPoint {
    fun getAppDataRepository(): AppDataRepository
    fun getRemoteDataRepository(): RemoteDataRepository
    fun getEventSyncManager(): EventSyncManager
    fun getSyncHandler(): SyncHandler
    fun getDeviceAuthLocalDataSource(): DeviceAuthLocalDataSource
} 