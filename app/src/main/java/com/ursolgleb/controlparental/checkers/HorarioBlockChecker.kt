package com.ursolgleb.controlparental.checkers

import com.ursolgleb.controlparental.data.apps.AppDataRepository
import com.ursolgleb.controlparental.utils.Fun
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HorarioBlockChecker @Inject constructor(
    private val appDataRepository: AppDataRepository
) {
    fun shouldBlock(packageName: String): Boolean {
        val app = appDataRepository.horarioAppsFlow.value.firstOrNull { it.packageName == packageName }
            ?: return false

        val horario = appDataRepository.horariosFlow.value.firstOrNull { it.nombreDeHorario == app.appName }
            ?: return false // si no tiene horario asignado, no bloquear

        val ahora = Fun.getHoraActual() // formato HH:mm
        val dia = Fun.getDiaDeLaSemana() // 1 = lunes ... 7 = domingo

        val estaDentroDelBloqueo = dia in horario.diasDeSemana &&
                Fun.estaDentroDelHorario(ahora, horario.horaInicio, horario.horaFin)

        return estaDentroDelBloqueo // bloquear si estamos dentro del horario
    }
}
