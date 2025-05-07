package com.ursolgleb.controlparental.data.apps

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.ursolgleb.controlparental.data.apps.dao.UsageEventDao
import com.ursolgleb.controlparental.data.apps.entities.UsageEventEntity
import com.ursolgleb.controlparental.utils.Fun
import com.ursolgleb.controlparental.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageEventProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageEventDao: UsageEventDao
) {

    suspend fun saveUsEventsUltimoMesToDatabase() {
        Logger.info(context, "UsageEventProvider", "saveUsEventsUltimoMesToDatabase Start...")
        val lastSaved = usageEventDao.getLastTimestamp() ?: 0L
        val cutoff = Fun.getTimeAtras(30)
        val startTime = if (lastSaved < cutoff) {
            usageEventDao.deleteOldEvents(cutoff)
            cutoff
        } else lastSaved + 1
        val endTime = System.currentTimeMillis()
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        val usageEvents = usageStatsManager?.queryEvents(startTime, endTime) ?: return
        val list = mutableListOf<UsageEventEntity>()
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.DEVICE_SHUTDOWN,
                UsageEvents.Event.DEVICE_STARTUP,
                UsageEvents.Event.ACTIVITY_RESUMED,
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    list.add(
                        UsageEventEntity(
                            packageName = event.packageName,
                            eventType = event.eventType,
                            timestamp = event.timeStamp
                        )
                    )
                }
            }
        }
        usageEventDao.insertAll(list)
        Logger.info(
            context,
            "UsageEventProvider",
            "saveUsEventsUltimoMesToDatabase End, events: ${list.size}"
        )
    }

    suspend fun getEventsFromDatabase(startTime: Long, endTime: Long): List<UsageEventEntity> {
        saveUsEventsUltimoMesToDatabase()
        return usageEventDao.getEvents(startTime, endTime)
    }

    suspend fun getEventsFromDatabase(
        startTime: Long,
        endTime: Long,
        listaApps: List<String>
    ): List<UsageEventEntity> {
        saveUsEventsUltimoMesToDatabase()
        return usageEventDao.getEvents(startTime, endTime, listaApps)
    }


}