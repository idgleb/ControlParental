package com.ursolgleb.controlparental.data.apps.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ursolgleb.controlparental.data.apps.entities.UsageEventEntity
import com.ursolgleb.controlparental.data.apps.entities.AppWithUsageEvents
import androidx.room.Transaction

@Dao
interface UsageEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<UsageEventEntity>)

    @Query("SELECT * FROM usage_events WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getEvents(startTime: Long, endTime: Long): List<UsageEventEntity>

    @Query("""
    SELECT * FROM usage_events 
    WHERE timestamp BETWEEN :startTime AND :endTime 
    AND packageName IN (:packageNames)
""")
    suspend fun getEvents(startTime: Long, endTime: Long, packageNames: List<String>): List<UsageEventEntity>


    @Query("DELETE FROM usage_events WHERE timestamp < :threshold")
    suspend fun deleteOldEvents(threshold: Long)

    @Query("SELECT MAX(timestamp) FROM usage_events")
    suspend fun getLastTimestamp(): Long?

    @Transaction
    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    suspend fun getAppWithEvents(packageName: String): AppWithUsageEvents?

    @Transaction
    @Query("SELECT * FROM apps")
    suspend fun getAllAppsWithEvents(): List<AppWithUsageEvents>


}
