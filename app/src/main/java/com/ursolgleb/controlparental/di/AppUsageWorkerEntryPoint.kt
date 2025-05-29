package com.ursolgleb.controlparental.di

import com.ursolgleb.controlparental.data.apps.AppDataRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppUsageWorkerEntryPoint {
    fun getAppDataRepository(): AppDataRepository
}
