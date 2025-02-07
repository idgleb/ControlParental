package com.ursolgleb.controlparental.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface BlockedAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertBlockedApp(app: BlockedAppEntity)

    @Query("SELECT * FROM blocked_apps ORDER BY blockedAt DESC")
    fun getBlockedApps(): List<BlockedAppEntity>

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName LIMIT 1")
    fun getBlockedApp(packageName: String): BlockedAppEntity?

    @Update
    fun updateBlockedApp(app: BlockedAppEntity)

    @Query("DELETE FROM blocked_apps")
    fun deleteAllBlockedApps()

}
