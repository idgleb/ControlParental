package com.ursolgleb.controlparental.data.apps.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ursolgleb.controlparental.data.apps.entities.AppEntity
import com.ursolgleb.controlparental.data.apps.entities.AppHorarioCrossRef
import com.ursolgleb.controlparental.data.apps.entities.AppWithHorarios
import com.ursolgleb.controlparental.data.apps.entities.HorarioEntity
import com.ursolgleb.controlparental.data.apps.entities.HorarioWithApps

@Dao
interface AppHorarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(ref: AppHorarioCrossRef)

    @Delete
    suspend fun deleteCrossRef(ref: AppHorarioCrossRef)

    @Transaction
    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    suspend fun getHorariosForApp(packageName: String): AppWithHorarios?

    @Transaction
    @Query("SELECT * FROM horarios WHERE id = :horarioId")
    suspend fun getAppsForHorario(horarioId: Int): HorarioWithApps?
}