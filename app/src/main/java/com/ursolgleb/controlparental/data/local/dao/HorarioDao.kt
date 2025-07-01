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

    @Query("DELETE FROM horarios WHERE deviceId = :deviceId")
    suspend fun deleteHorariosByDeviceId(deviceId: String)

    @Query("SELECT * FROM horarios WHERE idHorario = :idHorario AND deviceId = :deviceId LIMIT 1")
    suspend fun getHorarioByIdOnce(idHorario: Long, deviceId: String): HorarioEntity?

    @Query("SELECT * FROM horarios WHERE deviceId = :deviceId")
    suspend fun getHorariosByDeviceIdOnce(deviceId: String): List<HorarioEntity>

    @Query("DELETE FROM horarios WHERE idHorario = :idHorario")
    suspend fun deleteHorarioById(idHorario: Long)

    @Query("DELETE FROM horarios WHERE idHorario IN (:ids)")
    suspend fun deleteHorariosByIds(ids: List<Long>)

    @Update
    suspend fun updateHorario(horario: HorarioEntity)

    @Transaction
    suspend fun insertOrUpdateHorarios(horarios: List<HorarioEntity>) {
        horarios.forEach { horario ->
            val existing = getHorarioByIdOnce(horario.idHorario, horario.deviceId)
            if (existing != null) {
                updateHorario(horario)
            } else {
                insertHorario(horario)
            }
        }
    }

    // Comentados temporalmente - no se están usando
    // @Query("UPDATE horarios SET isActive = :isActive WHERE idHorario = :horarioId")
    // suspend fun setHorarioActive(horarioId: Long, isActive: Boolean)

    // @Query("UPDATE horarios SET isActive = :isActive")
    // suspend fun setAllHorariosActive(isActive: Boolean)
}
