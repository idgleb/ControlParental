package com.ursolgleb.controlparental.data.apps.dao

import androidx.room.*
import com.ursolgleb.controlparental.data.apps.entities.HorarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HorarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHorario(horario: HorarioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHorarios(horarios: List<HorarioEntity>)

    @Query("SELECT * FROM horarios ORDER BY idHorario")
    fun getAllHorarios(): Flow<List<HorarioEntity>>

    @Query("SELECT * FROM horarios ORDER BY idHorario")
    suspend fun getAllHorariosOnce(): List<HorarioEntity>

    @Query("SELECT * FROM horarios WHERE nombreDeHorario = :nombre LIMIT 1")
    suspend fun getHorarioPorNombre(nombre: String): HorarioEntity?

    @Delete
    suspend fun deleteHorario(horario: HorarioEntity)

    @Query("DELETE FROM horarios")
    suspend fun deleteAllHorarios()

    @Query("DELETE FROM horarios WHERE idHorario = :idHorario AND deviceId = :deviceId")
    suspend fun deleteByIdHorario(idHorario: Long, deviceId: String)

    // Comentados temporalmente - no se están usando
    // @Query("UPDATE horarios SET isActive = :isActive WHERE idHorario = :horarioId")
    // suspend fun setHorarioActive(horarioId: Long, isActive: Boolean)

    // @Query("UPDATE horarios SET isActive = :isActive")
    // suspend fun setAllHorariosActive(isActive: Boolean)
}
