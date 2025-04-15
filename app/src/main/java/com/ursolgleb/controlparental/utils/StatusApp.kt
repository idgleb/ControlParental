package com.ursolgleb.controlparental.utils

enum class StatusApp(val desc: String) {
    BLOQUEADA("BLOQUEADA"),
    DISPONIBLE("DISPONIBLE"),
    HORARIO("HORARIO"),
    DEFAULT("DEFAULT");

    companion object {
        fun fromDescription(desc: String): StatusApp {
            return entries.find { it.desc == desc } ?: DEFAULT
        }
    }
}
