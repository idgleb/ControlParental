package com.ursolgleb.controlparental.data.remote.models

import android.graphics.Bitmap

/**
 * DTO utilizado para sincronizar aplicaciones con el servidor
 * Usado en endpoints:
 * - GET /api/sync/apps?deviceId={deviceId}&limit={limit}&offset={offset}&includeIcons={bool} (dentro de PaginatedResponse<AppDto>)
 * - POST /api/sync/apps (array de AppDto)
 * - DELETE /api/sync/apps?deviceId={deviceIds}
 * - POST /api/sync/events (dentro del campo "data" de EventDto)
 * Ejemplo JSON:
 * {
 *   "packageName": "com.example.game",
 *   "deviceId": "abc-123-def-456",
 *   "appName": "Super Game",
 *   "appIcon": [137, 80, 78, 71, ...],
 *   "appCategory": "GAME",
 *   "contentRating": "Everyone",
 *   "isSystemApp": false,
 *   "usageTimeToday": 3600000,
 *   "timeStempUsageTimeToday": 1719648600000,
 *   "appStatus": "BLOQUEADA",
 *   "dailyUsageLimitMinutes": 60
 * }
 * 
 * Notas:
 * - appIcon: Array de bytes de la imagen PNG/JPEG convertidos a enteros (0-255)
 * - usageTimeToday: Tiempo de uso en millisegundos
 * - timeStempUsageTimeToday: Timestamp Unix cuando se actualizó el tiempo de uso
 * - appStatus: "DEFAULT", "DISPONIBLE", "BLOQUEADA"
 */
data class AppDto(
    val packageName: String?,              // ID único de la app (ej: com.example.app)
    val deviceId: String?,                 // ID del dispositivo donde está instalada
    val appName: String?,                  // Nombre visible de la app
    val appIcon: List<Int>?,               // Ícono como array de bytes (0-255)
    var appCategory: String?,              // Categoría de Play Store
    var contentRating: String?,            // Clasificación de contenido
    var isSystemApp: Boolean,              // Si es app del sistema
    var usageTimeToday: Long?,             // Tiempo de uso hoy en millis
    var timeStempUsageTimeToday: Long,     // Timestamp de última actualización
    val appStatus: String?,                // Estado de bloqueo
    val dailyUsageLimitMinutes: Int?,     // Límite diario en minutos
)
