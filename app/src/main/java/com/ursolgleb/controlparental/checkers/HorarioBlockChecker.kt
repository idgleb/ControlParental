package com.ursolgleb.controlparental.checkers

import android.util.Log
import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.utils.Fun
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
        Log.w("HorarioBlockChecker", "currentTime $currentTime")

        // 3. Obtener todos los horarios y verificar si alguno está activo y coincide
        val horariodDeApp = appDataRepository.horariosFlow.value

        Log.w("HorarioBlockChecker", "$horariodDeApp")

        return horariodDeApp.any { horario ->
            horario.isActive && // Solo considerar horarios activos
                    currentDay in horario.diasDeSemana &&
                    Fun.estaDentroDelHorario(currentTime, horario.horaInicio, horario.horaFin)
        }
    }
}
