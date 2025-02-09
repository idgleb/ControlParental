package com.ursolgleb.controlparental.data.log

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface LogBlockedAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogBlockedApp(app: LogBlockedAppEntity)

    @Query("SELECT * FROM log_blocked_apps ORDER BY blockedAt DESC")
    suspend fun getLogBlockedApps(): List<LogBlockedAppEntity>

    @Query("SELECT * FROM log_blocked_apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getLogBlockedApp(packageName: String): LogBlockedAppEntity?

    @Update
    suspend fun updateLogBlockedApp(app: LogBlockedAppEntity)

    @Query("DELETE FROM log_blocked_apps")
    suspend fun deleteLogAllBlockedApps()

}
