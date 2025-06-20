package com.ursolgleb.controlparental.data.apps.dao

import androidx.room.*
import com.ursolgleb.controlparental.data.apps.entities.HorarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HorarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHorario(horario: HorarioEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHorarios(horarios: List<HorarioEntity>)

    @Query("SELECT * FROM horarios ORDER BY id")
    fun getAllHorarios(): Flow<List<HorarioEntity>>

    @Query("SELECT * FROM horarios ORDER BY id")
    suspend fun getAllHorariosOnce(): List<HorarioEntity>

    @Query("SELECT * FROM horarios WHERE nombreDeHorario = :nombre LIMIT 1")
    suspend fun getHorarioPorNombre(nombre: String): HorarioEntity?

    @Delete
    suspend fun deleteHorario(horario: HorarioEntity)

    @Query("DELETE FROM horarios")
    suspend fun deleteAllHorarios()

    @Query("UPDATE horarios SET isActive = :isActive WHERE id = :horarioId")
    suspend fun setHorarioActive(horarioId: Int, isActive: Boolean)

    @Query("UPDATE horarios SET isActive = :isActive")
    suspend fun setAllHorariosActive(isActive: Boolean)
}
