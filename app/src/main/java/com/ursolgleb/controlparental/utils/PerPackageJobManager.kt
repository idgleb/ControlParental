package com.ursolgleb.controlparental.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PerPackageJobManager {

    private val jobMap = mutableMapOf<String, Job>()
    private val mutex = Mutex()

    suspend fun launchUniqueJob(
        scope: CoroutineScope,
        key: String,
        block: suspend CoroutineScope.() -> Unit
    ) {
        mutex.withLock {
            if (jobMap[key]?.isActive == true) return

            val job = scope.launch(block = block)

            job.invokeOnCompletion {
                scope.launch {
                    mutex.withLock {
                        jobMap.remove(key)
                    }
                }
            }

            jobMap[key] = job
        }
    }

    suspend fun isJobActive(key: String): Boolean {
        return mutex.withLock {
            jobMap[key]?.isActive == true
        }
    }

    suspend fun cancelJob(key: String) {
        mutex.withLock {
            jobMap[key]?.cancel()
            jobMap.remove(key)
        }
    }

    suspend fun cancelAll() {
        mutex.withLock {
            jobMap.values.forEach { it.cancel() }
            jobMap.clear()
        }
    }
}
