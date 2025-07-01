# API Endpoints - Control Parental

## Base URL
`https://tu-servidor.com/api/`

## Endpoints de Sincronización

### 1. Sincronización de Aplicaciones

#### GET /api/sync/apps
Obtiene la lista de aplicaciones para un dispositivo.

**Query Parameters:**
- `deviceId` (string): ID del dispositivo
- `limit` (int, default: 100): Número máximo de resultados
- `offset` (int, default: 0): Offset para paginación
- `includeIcons` (boolean, default: true): Si incluir íconos

**Response:** `PaginatedResponse<AppDto>`

#### POST /api/sync/apps
Envía una lista de aplicaciones al servidor.

**Request Body:** `List<AppDto>`

#### DELETE /api/sync/apps
Elimina aplicaciones por dispositivo.

**Query Parameters:**
- `deviceId` (List<string>): Lista de IDs de dispositivos

---

### 2. Sincronización de Horarios

#### GET /api/sync/horarios
Obtiene los horarios de un dispositivo.

**Query Parameters:**
- `deviceId` (string): ID del dispositivo
- `lastSync` (string, optional): Timestamp de última sincronización
- `knownIds` (string, optional): IDs conocidos

**Response:** `Response<SyncResponse<HorarioDto>>`

#### POST /api/sync/horarios
Envía una lista de horarios al servidor.

**Request Body:** `List<HorarioDto>`

#### DELETE /api/sync/horarios
Elimina horarios por dispositivo.

**Query Parameters:**
- `deviceId` (List<string>): Lista de IDs de dispositivos

---

### 3. Sincronización de Dispositivos

#### GET /api/sync/devices
Obtiene información del dispositivo.

**Query Parameters:**
- `deviceId` (string): ID del dispositivo

**Response:** `Response<List<DeviceDto>>`

#### POST /api/sync/devices
Actualiza información del dispositivo.

**Request Body:** `DeviceDto`

---

### 4. Sistema de Eventos (Principal)

#### GET /api/sync/events
Obtiene eventos pendientes de sincronización.

**Query Parameters:**
- `deviceId` (string): ID del dispositivo
- `lastEventId` (long): ID del último evento procesado
- `types` (string): Tipos de eventos separados por coma (ej: "horario,app")

**Response:** `SyncEventsResponse`

#### POST /api/sync/events
Envía eventos locales al servidor.

**Request Body:** `PostEventsRequest`
```json
{
  "deviceId": "abc-123",
  "events": [...]
}
```

---

### 5. Estado de Sincronización

#### GET /api/sync/status
Verifica el estado de sincronización del dispositivo.

**Query Parameters:**
- `deviceId` (string): ID del dispositivo

**Response:** `SyncStatusResponse`
```json
{
  "status": "success",
  "deviceId": "abc-123",
  "pendingEvents": {"horario": 3, "app": 5},
  "lastEventId": 42,
  "lastEventTime": "2025-06-29T10:30:00Z",
  "serverTime": "2025-06-29T10:35:00Z"
}
```

---

## Notas Importantes

1. **Autenticación**: Todos los endpoints requieren autenticación (bearer token).
2. **Formato de Fecha**: Usar ISO 8601 con zona horaria UTC (ej: "2025-06-29T10:30:00Z").
3. **IDs de Dispositivo**: Usar Android ID o UUID único por dispositivo.
4. **Manejo de Errores**: 
   - 400: Bad Request (parámetros inválidos)
   - 401: Unauthorized (token inválido)
   - 422: Unprocessable Entity (validación fallida)
   - 500: Internal Server Error

## Flujo de Sincronización Recomendado

1. Verificar estado con `GET /api/sync/status`
2. Obtener eventos pendientes con `GET /api/sync/events`
3. Aplicar eventos recibidos localmente
4. Enviar eventos locales con `POST /api/sync/events`
5. Repetir hasta que no haya más eventos pendientes 