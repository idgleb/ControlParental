package com.ursolgleb.controlparental.checkers

import com.ursolgleb.controlparental.data.apps.AppDataRepository
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TiempoUsoBlockChecker @Inject constructor(
    private val appDataRepository: AppDataRepository
) {
    fun shouldBlock(packageName: String): Boolean = runBlocking {
        val limit = appDataRepository.appDatabase.usageLimitDao().getLimit(packageName) ?: return@runBlocking false
        val app = appDataRepository.todosAppsFlow.value.firstOrNull { it.packageName == packageName } ?: return@runBlocking false

        val usadoMinutos = app.usageTimeToday / 60000
        return@runBlocking usadoMinutos >= limit.maxUsageMinutesPorDia
    }
}
