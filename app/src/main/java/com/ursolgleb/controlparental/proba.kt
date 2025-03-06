package com.ursolgleb.controlparental

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.ursolgleb.controlparental.data.local.AppDatabase
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class proba @Inject constructor(
    val appDatabase: AppDatabase,
    val context: Context
) {

    fun getTiempoDeUsoSeconds(packageName: String, dias: Int): Long {
        val startTime = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -dias)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endTime = System.currentTimeMillis()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return 0

        // Obtener el tiempo total en foreground sin contar la última sesión abierta
        val totalTimeInForegroundSinUltimaSesionAbierta =
            usageStatsManager.queryAndAggregateUsageStats(
                startTime,
                endTime
            )[packageName]?.totalTimeInForeground?.div(1000) ?: 0

        // Obtener eventos de uso en el rango de tiempo
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()

        var lastForegroundTimestamp = 0L

        // Recorrer eventos
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (event.packageName == packageName && event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastForegroundTimestamp = event.timeStamp
            }
        }

        // Si la app sigue en foreground, calcular tiempo adicional
        val timeAdicionalParaAppsEnSeccionAbierta =
            if (lastForegroundTimestamp > 0 && lastForegroundTimestamp < endTime) {
                (endTime - lastForegroundTimestamp) / 1000
            } else {
                0L
            }

        return totalTimeInForegroundSinUltimaSesionAbierta + timeAdicionalParaAppsEnSeccionAbierta
    }

}
