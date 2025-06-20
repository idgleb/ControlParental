package com.ursolgleb.controlparental.di

import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.data.apps.dao.HorarioDao
import com.ursolgleb.controlparental.data.remote.RemoteDataRepository
import com.ursolgleb.controlparental.handlers.SyncHandler
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun getAppDataRepository(): AppDataRepository
    fun getRemoteDataRepository(): RemoteDataRepository
    fun getSyncHandler(): SyncHandler
    fun getHorarioDao(): HorarioDao
}