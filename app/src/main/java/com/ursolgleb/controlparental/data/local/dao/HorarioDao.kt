package com.ursolgleb.controlparental.data.local.dao

import androidx.room.*
import com.ursolgleb.controlparental.data.local.entities.HorarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HorarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHorario(horario: HorarioEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHorarios(horarios: List<HorarioEntity>)

    @Query("SELECT * FROM horarios ORDER BY nombreDeHorario")
    fun getAllHorarios(): Flow<List<HorarioEntity>>

    @Query("SELECT * FROM horarios WHERE nombreDeHorario = :nombre LIMIT 1")
    suspend fun getHorarioPorNombre(nombre: String): HorarioEntity?

    @Delete
    suspend fun deleteHorario(horario: HorarioEntity)

    @Query("DELETE FROM horarios")
    suspend fun deleteAll()
}
