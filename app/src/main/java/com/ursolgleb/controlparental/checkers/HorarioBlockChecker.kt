package com.ursolgleb.controlparental.checkers

import android.util.Log
import com.ursolgleb.controlparental.data.apps.dao.HorarioDao
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.utils.Fun
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HorarioBlockChecker @Inject constructor(
    private val appDataRepository: AppDataRepository
) {
    fun shouldBlock(packageName: String): Boolean {
        // 1. Verificar si la app tiene status HORARIO
        appDataRepository.horarioAppsFlow.value.firstOrNull { it.packageName == packageName }
            ?: return false

        val currentTime = Fun.getHoraActual()
        val currentDay = Fun.getDiaDeLaSemana() // 1 = lunes, ..., 7 = domingo
        Log.w("HorarioBlockChecker", "currentDay $currentDay")

        // 3. Obtener todos los horarios y verificar si alguno está activo y coincide
        val horarios = appDataRepository.horariosFlow.value

        val horariodDeApp = appDataRepository.getHorariosPorPkg(packageName)


        return horarios.any { horario ->
            horario.isActive && // Solo considerar horarios activos
                    currentDay in horario.diasDeSemana &&
                    Fun.estaDentroDelHorario(currentTime, horario.horaInicio, horario.horaFin)
        }
    }
}
