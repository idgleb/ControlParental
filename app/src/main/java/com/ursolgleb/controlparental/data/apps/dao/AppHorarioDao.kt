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
import kotlinx.coroutines.flow.Flow

@Dao
interface AppHorarioDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(ref: AppHorarioCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(refs: List<AppHorarioCrossRef>)

    @Delete
    suspend fun deleteCrossRef(ref: AppHorarioCrossRef)

    @Query("DELETE FROM app_horario_cross_ref WHERE packageName IN (:packageNames)")
    suspend fun deleteCrossRefsByPackage(packageNames: List<String>)



    @Transaction
    @Query("SELECT * FROM apps WHERE packageName = :packageName")
    fun getHorariosForAppFlow(packageName: String): Flow<AppWithHorarios?>

    @Transaction
    @Query("SELECT * FROM horarios WHERE id = :horarioId")
    fun getAppsForHorarioFlow(horarioId: Int): Flow<HorarioWithApps?>


    // Función para obtener todos los apps con sus horarios
    @Transaction
    @Query("SELECT * FROM apps")
    fun getAllAppsWithHorariosFlow(): Flow<List<AppWithHorarios>>

    // Función para obtener todos los horarios con sus apps
    @Transaction
    @Query("SELECT * FROM horarios")
    fun getAllHorariosWithAppsFlow(): Flow<List<HorarioWithApps>>
}