package com.ursolgleb.controlparental.data.apps.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ursolgleb.controlparental.data.apps.entities.UsageLimitEntity

@Dao
interface UsageLimitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLimit(limit: UsageLimitEntity)

    @Query("SELECT * FROM usage_limits WHERE packageName = :packageName")
    suspend fun getLimit(packageName: String): UsageLimitEntity?

    @Query("DELETE FROM usage_limits WHERE packageName = :packageName")
    suspend fun deleteLimit(packageName: String)
}
