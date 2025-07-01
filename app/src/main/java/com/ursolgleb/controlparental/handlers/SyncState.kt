package com.ursolgleb.controlparental.handlers

/**
 * Estados posibles de sincronización
 */
enum class SyncState {
    IDLE,           // Sin actividad
    SYNCING,        // Sincronizando
    SUCCESS,        // Última sincronización exitosa
    ERROR,          // Error en la última sincronización
    PENDING_EVENTS  // Hay eventos pendientes de enviar
} 