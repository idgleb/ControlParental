package com.ursolgleb.controlparental.data.local.providers

import android.app.usage.UsageEvents
import android.content.Context
import com.ursolgleb.controlparental.utils.Fun
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.reactivecircus.cache4k.Cache
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Singleton
class UsageTimeProvider @Inject constructor(
    private val usageEventProvider: UsageEventProvider,
    @ApplicationContext private val context: Context,
) {

    private val cache = Cache.Builder()
        .expireAfterWrite(4.toDuration(DurationUnit.SECONDS))
        .maximumCacheSize(50)
        .build<List<String>, Map<String, Long>>()

    suspend fun <T> getTiempoDeUsoHoy(
        apps: List<T>,
        getPkg: (T) -> String
    ): Map<String, Long> {
        val key = apps.map(getPkg)
        return cache.get(key) {
            calculateTiempoDeUsoHoy(key)
        }
    }

    private suspend fun calculateTiempoDeUsoHoy(
        appsPackageNames: List<String>
    ): Map<String, Long> {
        val startTimeHoy = Fun.getTimeAtras(0) // "Hoy a las 00:00:00"
        val endTimeAhora = System.currentTimeMillis() // tiempo actual
        val statsMap = mutableMapOf<String, Long>()
        val active = mutableMapOf<String, Long>() // packageName -> timestamp de inicio
        val lastTwo = mutableMapOf<String, MutableList<Int>>()
        // obtenemos y procesamos eventos SOLO de estos paquetes
        val events = usageEventProvider.getEventsFromDatabase(startTimeHoy, endTimeAhora, appsPackageNames)
        events
            .sortedBy { it.timestamp }
            .forEach { ev ->
                val evListForApp = lastTwo.getOrPut(ev.packageName) { mutableListOf() }
                evListForApp.add(ev.eventType)
                if (evListForApp.size > 3) evListForApp.removeAt(0)
                when (ev.eventType) {
                    //1
                    UsageEvents.Event.ACTIVITY_RESUMED ->
                    // Registrar el inicio de la sesión de esta app
                    if (active[ev.packageName] == null) active[ev.packageName] = ev.timestamp
                    //23
                    UsageEvents.Event.ACTIVITY_STOPPED -> {
                        // si primero evento ACTIVITY_STOPPED es significa que la app estaba abierta,
                        // por eso sumamos desde el inicio hasta eventTime
                        statsMap.getOrPut(ev.packageName) { 0L }
                        if (evListForApp.size == 1) {
                            statsMap[ev.packageName] =
                                (statsMap[ev.packageName] ?: 0L) + (ev.timestamp - startTimeHoy)
                        }
                        // si ultimos dos eventos es ACTIVITY_PAUSED,ACTIVITY_RESUMED es incorrecto
                        // por eso no cerrar la sesion
                        if (!(lastTwo[ev.packageName]?.get(0) == UsageEvents.Event.ACTIVITY_PAUSED
                                    && lastTwo[ev.packageName]?.get(1) == UsageEvents.Event.ACTIVITY_RESUMED)
                        ) {
                            // Cerrar la sesión de esta app si estaba activa
                            active[ev.packageName]?.let { start ->
                                statsMap.getOrPut(ev.packageName) { 0L }
                                statsMap[ev.packageName] =
                                    (statsMap[ev.packageName] ?: 0L) + (ev.timestamp - start)
                                active.remove(ev.packageName)
                            }
                        }
                    }
                    // 26, 27
                    UsageEvents.Event.DEVICE_SHUTDOWN, UsageEvents.Event.DEVICE_STARTUP -> {
                        // Cerrar las sesiónes de TODAS las apps activas
                        active.forEach { (pkg, start) ->
                            statsMap.getOrPut(pkg) { 0L }
                            statsMap[pkg] = (statsMap[pkg] ?: 0L) + (ev.timestamp - start)
                        }
                        active.clear()
                    }
                }
            }
        //agregamos tiempo para apps que todavia estan abiertas hasta el tiempo actual
        active.forEach { (pkg, start) ->
            statsMap.getOrPut(pkg) { 0L }
            statsMap[pkg] = (statsMap[pkg] ?: 0L) + (endTimeAhora - start)
        }
        return statsMap
    }


}