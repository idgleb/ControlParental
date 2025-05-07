package com.ursolgleb.controlparental.data.apps

import android.app.usage.UsageEvents
import android.content.Context
import com.ursolgleb.controlparental.utils.Fun
import com.ursolgleb.controlparental.utils.Logger
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
        .expireAfterWrite(15.toDuration(DurationUnit.SECONDS))
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


    suspend fun getTiempoDeUsoHoy(): Map<String, Long> {
        val startTimeHoy = Fun.getTimeAtras(0)
        val endTimeAhora = System.currentTimeMillis()
        Logger.info(
            context,
            "AppDataRepository",
            "getTiempoDeUsoHoyAll desde ${Fun.dateFormat.format(startTimeHoy)}"
        )
        val statsMap = mutableMapOf<String, Long>()
        val events = usageEventProvider.getEventsFromDatabase(startTimeHoy, endTimeAhora)
        val active = mutableMapOf<String, Long>()
        val lastTwo = mutableMapOf<String, MutableList<Int>>()

        events.sortedBy { it.timestamp }.forEach { event ->
            val evList = lastTwo.getOrPut(event.packageName) { mutableListOf() }
            evList.add(event.eventType)
            if (evList.size > 3) evList.removeAt(0)

            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> active[event.packageName] = event.timestamp
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (evList.size == 1) {
                        statsMap[event.packageName] =
                            (statsMap[event.packageName] ?: 0L) + (event.timestamp - startTimeHoy)
                    }
                    if (!(lastTwo[event.packageName]?.let { it[0] == UsageEvents.Event.ACTIVITY_PAUSED && it[1] == UsageEvents.Event.ACTIVITY_RESUMED } == true)) {
                        active.remove(event.packageName)?.let { start ->
                            statsMap[event.packageName] =
                                (statsMap[event.packageName] ?: 0L) + (event.timestamp - start)
                        }
                    }
                }

                UsageEvents.Event.DEVICE_SHUTDOWN, UsageEvents.Event.DEVICE_STARTUP -> {
                    active.forEach { (pkg, start) ->
                        statsMap[pkg] = (statsMap[pkg] ?: 0L) + (event.timestamp - start)
                    }
                    active.clear()
                }
            }
        }
        active.forEach { (pkg, start) ->
            statsMap[pkg] = (statsMap[pkg] ?: 0L) + (endTimeAhora - start)
        }
        Logger.info(context, "AppDataRepository", "statsMapHoyAll calculado: $statsMap")
        return statsMap
    }


}