# Skip Today - Documento Tecnico

## Estructura del Proyecto

```
app/src/main/
├── AndroidManifest.xml          # Permisos, activities, services, receivers
├── java/com/groupalarm/app/
│   ├── GroupAlarmApp.kt         # Application: inicializa canal de notificaciones
│   ├── MainActivity.kt          # Entry point: pide permisos y lanza NavGraph
│   ├── alarm/                   # Sistema de alarmas
│   │   ├── AlarmScheduler.kt    # Programa/cancela alarmas en AlarmManager
│   │   ├── AlarmReceiver.kt     # Recibe el evento del sistema y lanza el service
│   │   ├── AlarmService.kt      # Foreground service: sonido, vibracion, notificacion
│   │   ├── AlarmActivity.kt     # Pantalla full-screen cuando suena la alarma
│   │   ├── AlarmActionReceiver.kt # Procesa acciones desde la notificacion
│   │   └── BootReceiver.kt      # Reprograma alarmas tras reinicio del telefono
│   ├── data/
│   │   ├── model/
│   │   │   ├── Alarm.kt         # Entidad: hora, minuto, dias, sonido, grupo
│   │   │   ├── AlarmGroup.kt    # Entidad: nombre, silencedDate
│   │   │   └── AlarmWithGroup.kt # Relacion Room alarm+grupo
│   │   ├── dao/
│   │   │   ├── AlarmDao.kt      # Queries SQL para alarmas
│   │   │   └── AlarmGroupDao.kt # Queries SQL para grupos
│   │   ├── db/
│   │   │   └── AppDatabase.kt   # Base de datos Room (singleton)
│   │   └── repository/
│   │       └── AlarmRepository.kt # Capa de acceso a datos unificada
│   ├── di/
│   │   └── AppModule.kt         # Hilt: provee database y DAOs
│   └── ui/
│       ├── theme/
│       │   ├── Theme.kt         # Material 3 con colores dinamicos
│       │   └── Color.kt         # Constantes de color
│       ├── home/
│       │   ├── HomeScreen.kt    # Pantalla principal: lista de grupos y alarmas
│       │   └── HomeViewModel.kt # Logica: CRUD alarmas/grupos, toggle silence
│       ├── edit/
│       │   ├── EditAlarmScreen.kt  # Formulario crear/editar alarma
│       │   └── EditAlarmViewModel.kt # Logica: guardar/eliminar alarma
│       └── navigation/
│           └── NavGraph.kt      # Rutas: home, edit/{alarmId}
└── res/
    ├── values/strings.xml       # Nombre de la app
    ├── values/colors.xml        # Color del icono
    ├── drawable/                 # Iconos vectoriales del launcher
    └── mipmap-anydpi-v26/       # Icono adaptativo
```

## Modelo de Datos

### Tabla `alarms`

| Campo       | Tipo    | Descripcion                                              |
|-------------|---------|----------------------------------------------------------|
| id          | Long PK | Autogenerado                                             |
| group_id    | Long FK | Referencia a alarm_groups (CASCADE on delete)            |
| hour        | Int     | 0-23                                                     |
| minute      | Int     | 0-59                                                     |
| days_of_week| Int     | Bitmask: Lun=1, Mar=2, Mie=4, Jue=8, Vie=16, Sab=32, Dom=64. 0=una vez |
| is_enabled  | Boolean | Si la alarma esta activa                                 |
| sound_uri   | String  | URI del tono del sistema                                 |
| label       | String  | Etiqueta opcional                                        |

### Tabla `alarm_groups`

| Campo         | Tipo    | Descripcion                                            |
|---------------|---------|--------------------------------------------------------|
| id            | Long PK | Autogenerado                                           |
| name          | String  | Nombre del grupo                                       |
| silenced_date | String? | Fecha ISO (yyyy-MM-dd) del dia silenciado, null si activo |

### Mecanismo "Silenciar por hoy"

`silenced_date` guarda la fecha de hoy como string (ej: `"2026-02-27"`). Antes de que suene una alarma, el servicio compara `silencedDate == LocalDate.now().toString()`. Si coincide, la alarma no suena. Al dia siguiente la fecha ya no coincide y todo vuelve a la normalidad sin necesidad de ningun reseteo.

## Flujos Principales

### Crear alarma

```
HomeScreen (FAB +)
  → NavGraph → EditAlarmScreen (alarmId = -1)
    → Usuario configura hora, dias, grupo, sonido, etiqueta
      → EditAlarmViewModel.save()
        → AlarmRepository.insertAlarm() → Room DB
        → AlarmScheduler.schedule() → AlarmManager.setAlarmClock()
          → PendingIntent apuntando a AlarmReceiver
```

### Alarma suena

```
AlarmManager (hora programada)
  → AlarmReceiver.onReceive()
    → startForegroundService(AlarmService)
      → AlarmService.onStartCommand()
        1. startForeground() inmediatamente (obligatorio Android 14+)
        2. Coroutine IO:
           a. Carga alarma + grupo de la DB
           b. Verifica si grupo esta silenciado hoy
              - Si: reprograma siguiente ocurrencia, stopSelf(), no suena
              - No: continua
           c. Actualiza notificacion con datos reales
           d. Reproduce sonido (MediaPlayer, fallback a Ringtone API)
           e. Activa vibracion
           f. Reprograma alarma repetitiva para siguiente ocurrencia
           g. Lanza AlarmActivity (pantalla full-screen)
```

### Acciones del usuario cuando suena

| Accion          | Desde Activity          | Desde Notificacion       |
|-----------------|-------------------------|--------------------------|
| **Apagar**      | stopService + finish    | AlarmActionReceiver → stop + broadcast finish |
| **Snooze 5min** | scheduleSnooze + finish | AlarmActionReceiver → scheduleSnooze + broadcast finish |
| **Basta por hoy** | silenceGroupForDate + finish | AlarmActionReceiver → silenceGroupForDate + broadcast finish |

Las tres acciones siempre: paran el servicio, cancelan la notificacion, y cierran la activity.

### Silenciar grupo desde la pantalla principal

```
HomeScreen → icono campanita en GroupCard
  → HomeViewModel.toggleGroupSilence(group)
    - Si ya esta silenciado: updateGroup(silencedDate = null)
    - Si no: silenceGroupForToday(groupId) → guarda fecha de hoy
```

### Despues de reiniciar el telefono

```
Sistema envia ACTION_BOOT_COMPLETED
  → BootReceiver.onReceive()
    → Lee todas las alarmas habilitadas de la DB
    → AlarmScheduler.schedule() para cada una
```

## Programacion de Alarmas

`AlarmScheduler` usa `AlarmManager.setAlarmClock()` que:
- Esta exento de restricciones de alarmas exactas
- Garantiza que suena aunque el telefono este en Doze mode
- Solo programa UNA ocurrencia a la vez

Cada vez que una alarma suena, si tiene dias de repeticion, el servicio la reprograma para la siguiente ocurrencia. `getNextTriggerTime()` busca el proximo dia habilitado en el bitmask dentro de los siguientes 7 dias.

## Inyeccion de Dependencias (Hilt)

```
GroupAlarmApp (@HiltAndroidApp)
  └── AppModule (@Module)
      ├── provideDatabase() → AppDatabase singleton
      ├── provideAlarmDao()
      └── provideAlarmGroupDao()

Inyectados automaticamente:
  ├── AlarmRepository (@Singleton)
  ├── AlarmScheduler (@Singleton)
  ├── HomeViewModel (@HiltViewModel)
  └── EditAlarmViewModel (@HiltViewModel)
```

Los componentes que no pueden usar Hilt (BroadcastReceivers, Service) acceden a la DB via `AppDatabase.getInstance(context)` y crean `AlarmScheduler(context)` directamente.

## Permisos

| Permiso                        | Para que                                          |
|--------------------------------|---------------------------------------------------|
| USE_EXACT_ALARM                | Programar alarmas exactas (Android 14+)           |
| SCHEDULE_EXACT_ALARM           | Programar alarmas exactas (Android 12-13)         |
| POST_NOTIFICATIONS             | Mostrar notificaciones (Android 13+)              |
| USE_FULL_SCREEN_INTENT         | Pantalla completa sobre lock screen               |
| FOREGROUND_SERVICE             | Mantener el sonido reproduciendo                  |
| FOREGROUND_SERVICE_MEDIA_PLAYBACK | Tipo de foreground service requerido Android 14+ |
| RECEIVE_BOOT_COMPLETED         | Reprogramar alarmas tras reinicio                 |
| VIBRATE                        | Vibracion de alarma                               |
| WAKE_LOCK                      | Mantener el dispositivo despierto                 |

## Stack Tecnologico

- **Lenguaje**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **Base de datos**: Room (SQLite)
- **DI**: Hilt (Dagger)
- **Navegacion**: Navigation Compose
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
