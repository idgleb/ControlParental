package com.ursolgleb.controlparental.data.apps

import android.app.usage.UsageStatsManager
import android.content.Context
import com.ursolgleb.controlparental.data.apps.dao.UsageStatsDao
import com.ursolgleb.controlparental.data.apps.entities.UsageStatsEntity
import com.ursolgleb.controlparental.utils.Fun
import com.ursolgleb.controlparental.utils.Logger
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.reactivecircus.cache4k.Cache
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Singleton
class UsageStatsProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsDao: UsageStatsDao
) {

    private val cache = Cache.Builder()
        .expireAfterWrite(1.toDuration(DurationUnit.MINUTES))
        .maximumCacheSize(50)
        .build<Pair<Long,Long>, Map<String, Map<Long,Long>>>()



    suspend fun saveUsStatsUltimaSemanaToDatabase() {
        Logger.info(context, "AppDataRepository", "saveUsStatsUltimoSemanaToDatabase Start...")
        val lastDia = usageStatsDao.getLastDia() ?: 0L
        val cutoff = Fun.getTimeAtras(7)
        var startTime = if (lastDia < cutoff) {
            usageStatsDao.deleteOldUsageStats(cutoff)
            cutoff
        } else lastDia + 24 * 60 * 60 * 1000
        val endTime = Fun.getTimeAtras(0)
        val list = mutableListOf<UsageStatsEntity>()
        val usageStats = getUsageStatsEnSistema(startTime, endTime)
        usageStats.forEach { (pkg, map) ->
            map.forEach { (day, dur) ->
                list.add(UsageStatsEntity(packageName = pkg, dia = day, usageDuration = dur))
            }
        }
        usageStatsDao.insertAllUsageStats(list)
        Logger.info(
            context,
            "AppDataRepository",
            "saveUsStatsUltimoSemanaToDatabase End, records: ${list.size}"
        )
    }

    suspend fun getStatsFromDatabase(startTime: Long, endTime: Long): List<UsageStatsEntity> {
        saveUsStatsUltimaSemanaToDatabase()
        return usageStatsDao.getAllUsageStats()
    }

    suspend fun getUsageStatsEnSistema(startTime: Long, endTime: Long): Map<String, Map<Long, Long>> {
        val key = startTime to endTime
        return cache.get(key) {
            computeUsageStatsEnSistema(startTime, endTime)
        }
    }


    private fun computeUsageStatsEnSistema(
        startTime: Long,
        endTime: Long
    ): Map<String, Map<Long, Long>> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val statsList = usageStatsManager
            .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            ?: emptyList()

        val statsMap = mutableMapOf<String, MutableMap<Long, Long>>()

        statsList.forEach { stats ->
            if (stats.lastTimeUsed !in startTime..endTime) return@forEach

            val cal = Calendar.getInstance().apply {
                timeInMillis = stats.lastTimeUsed
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val day = cal.timeInMillis

            val appMap = statsMap.getOrPut(stats.packageName) { mutableMapOf() }
            appMap[day] = (appMap[day] ?: 0L) + stats.totalTimeInForeground
        }

        Logger.info(context, "AppDataRepository", "getUsageStatsEnSistema calculado: $statsMap")
        // Convertimos a inmutables antes de devolver
        return statsMap.mapValues { it.value.toMap() }
    }

}