package com.ursolgleb.controlparental.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface BlockedAppDao {
    @Insert
    fun insertBlockedApp(app: BlockedAppEntity)

    @Query("SELECT * FROM blocked_apps")
    fun getBlockedApps(): List<BlockedAppEntity>

}
