package com.ursolgleb.controlparental.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ursolgleb.controlparental.data.local.entities.BlockedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBlockedApp(app: BlockedEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedApps(apps: List<BlockedEntity>)

    @Delete
    suspend fun deleteBlockedApp(app: BlockedEntity)

    @Query("SELECT * FROM blocked")
    fun getAllBlockedApps(): Flow<List<BlockedEntity>>

    @Query("DELETE FROM blocked")
    suspend fun deleteAllBlockedApps()

    @Query("SELECT COUNT(*) FROM blocked")
    suspend fun getBlockedAppsCount(): Int

    @Query("SELECT * FROM blocked WHERE packageName = :packageName")
    suspend fun getBlockedAppByPackageName(packageName: String): BlockedEntity?

}