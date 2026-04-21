# GestorRH - Cliente Android
 
[![Android CI](https://github.com/GestorRH-Multiplataforma/gestorrh-android/actions/workflows/android-ci.yml/badge.svg)](https://github.com/GestorRH-Multiplataforma/gestorrh-android/actions/workflows/android-ci.yml)
[![Version](https://img.shields.io/badge/version-v0.9.0--beta-yellow)](https://github.com/GestorRH-Multiplataforma/gestorrh-android/releases/tag/v0.9.0-beta)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android SDK](https://img.shields.io/badge/SDK-34%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
 
Cliente móvil nativo desarrollado para el ecosistema **GestorRH 2.0**. Esta aplicación permite a los empleados gestionar su jornada laboral, consultar turnos y tramitar ausencias mediante una interfaz moderna y reactiva, integrada con el backend centralizado [GestorRH-API](https://github.com/GestorRH-Multiplataforma/GestorRH-API).
 
> Este repositorio forma parte del ecosistema **GestorRH Multiplataforma**. Para entender el contexto general del proyecto (API, cliente escritorio y arquitectura global), consulta el [README de la organización](https://github.com/GestorRH-Multiplataforma#gestorrh---ecosistema-multiplataforma).
 
---
 
## Tecnologías Utilizadas
 
- **Lenguaje:** Kotlin 1.9.22
- **UI:** Jetpack Compose con Material Design 3
- **Arquitectura:** Clean Architecture + MVVM
- **Inyección de Dependencias:** Manual (ViewModelProvider.Factory)
- **Red:** Retrofit 2 & OkHttp 4
- **Persistencia Local:** Room Database (soporte offline)
- **Navegación:** Compose Navigation con rutas fuertemente tipadas
- **Concurrencia:** Kotlin Coroutines & Flow
- **CI/CD:** GitHub Actions
---
 
## Requisitos Previos
 
- **Android Studio** Iguana o superior
- **JDK 17**
- **[GestorRH-API](https://github.com/GestorRH-Multiplataforma/GestorRH-API)** en ejecución para el consumo de datos
---
 
## Estructura del Proyecto
 
```
app/src/main/java/com/gestorrh/android/
├── MainActivity.kt            # Entry point: inicializa dependencias globales, gestiona
│                              #   auto-login y observa AuthEventBus para redirigir ante 401
│
├── core/                      # Infraestructura transversal (sin lógica de negocio)
│   ├── location/
│   │   └── GestorLocalizacion.kt      # Wrapper de FusedLocationProviderClient
│   ├── navigation/
│   │   ├── BarraNavegacionInferior.kt # BottomBar dinámica inyectable por rol
│   │   └── RutasDestino.kt            # Sealed class con todas las rutas de la app
│   ├── network/
│   │   ├── ApiClient.kt               # Motor HTTP: Retrofit + OkHttp + Gson
│   │   ├── AuthInterceptor.kt         # Inyección de JWT e interceptación global de 401
│   │   ├── LocalDateDeserializer.kt   # Adaptador Gson para LocalDate
│   │   └── LocalDateTimeDeserializer.kt
│   ├── security/
│   │   ├── AuthEventBus.kt            # Canal SharedFlow para eventos de sesión expirada
│   │   └── SessionManager.kt         # Persistencia cifrada del JWT (EncryptedSharedPreferences)
│   └── ui/
│       └── MensajeUi.kt               # Abstracción de mensajes de error (Recurso / Dinámico)
│
├── data/                      # Capa de datos: fuentes locales y remotas
│   ├── local/
│   │   ├── GestorRhDatabase.kt        # Base de datos Room (singleton)
│   │   ├── dao/
│   │   │   └── AsignacionDao.kt       # DAO con upsert, observación reactiva y deleteAll
│   │   ├── entity/
│   │   │   └── AsignacionEntity.kt    # Entidad Room para caché offline de turnos
│   │   └── mapper/
│   │       └── AsignacionMapper.kt    # Conversión DTO → Entity con timestamp de sincronización
│   ├── network/
│   │   ├── asignacion/                # AsignacionApiService + DTOs
│   │   ├── ausencia/                  # AusenciaApiService + DTOs (multipart)
│   │   ├── autenticacion/             # AuthApi + PeticionLoginDTO + RespuestaLoginDTO
│   │   ├── empleado/                  # EmpleadoApi + DTOs de perfil y cambio de contraseña
│   │   └── fichaje/                   # FichajeApiService + DTOs de entrada, salida y estado BFF
│   └── repository/
│       ├── AuthRepository.kt          # Login → Result<RespuestaLoginDTO>
│       ├── FichajeRepository.kt       # Estado actual, entrada y salida → Result
│       ├── PerfilRepository.kt        # Perfil y cambio de contraseña → Result
│       ├── asignacion/
│       │   └── AsignacionRepositoryImpl.kt  # Estrategia offline-first con Room + Retrofit
│       └── ausencia/
│           └── AusenciaRepositoryImpl.kt    # Tipos + creación multipart → Result
│
├── domain/                    # Contratos puros (interfaces + casos de uso)
│   ├── repository/
│   │   ├── IAsignacionRepository.kt   # Flow reactivo + sincronización
│   │   ├── IAusenciaRepository.kt     # Tipos y creación de ausencias
│   │   ├── IAuthRepository.kt         # Autenticación
│   │   ├── IFichajeRepository.kt      # Estado actual, entrada y salida
│   │   ├── IPerfilRepository.kt       # Perfil y contraseña
│   │   └── ResultadoSincronizacion.kt # Sealed class: Exito / SinConexion / Error
│   └── usecase/
│       └── ausencia/
│           └── SolicitarAusenciaUseCase.kt  # Validación de fechas y tipo antes de enviar
│
└── ui/                        # Capa de presentación (Jetpack Compose + MVVM)
    ├── ausencia/
    │   ├── EstadoUiSolicitudAusencia.kt
    │   ├── PantallaSolicitudAusencia.kt
    │   └── SolicitudAusenciaViewModel.kt
    ├── dashboard/
    │   ├── DashboardViewModel.kt      # Cronómetro reactivo + orquestador de fichaje
    │   └── PantallaDashboard.kt
    ├── login/
    │   ├── EstadoUiLogin.kt
    │   ├── LoginViewModel.kt
    │   └── PantallaLogin.kt
    ├── perfil/
    │   ├── EstadoUiPerfil.kt
    │   ├── PantallaPerfil.kt
    │   └── PerfilViewModel.kt
    ├── principal/
    │   └── PantallaPrincipal.kt       # Scaffold maestro post-login con NavHost interno
    ├── theme/
    │   ├── Color.kt                   # Paleta corporativa (NavyPrimary + CyanSecondary)
    │   ├── Shape.kt
    │   ├── Theme.kt                   # Dark/Light con soporte de color dinámico desactivado
    │   └── Type.kt
    └── turnos/
        ├── EstadoUiMisTurnos.kt
        ├── MisTurnosViewModel.kt      # Offline-first: observa Room, sincroniza en background
        └── PantallaMisTurnos.kt       # Vista lista y calendario con BottomSheet de detalle
```
 
---
 
## Configuración del Entorno
 
### Variables de Configuración
 
La aplicación utiliza un archivo `secrets.properties` en la raíz del proyecto para gestionar las URLs del backend según el entorno. Este archivo **no se sube al repositorio** (incluido en `.gitignore`).
 
Crea el archivo manualmente con el siguiente contenido:
 
```properties
DEV_BASE_URL="http://10.0.2.2:8080/api/"
PROD_BASE_URL="https://tu-dominio.com/api/"
```
 
| Variable | Descripción | Valor por defecto (emulador) |
|---|---|---|
| `DEV_BASE_URL` | URL de la API en entorno de desarrollo | `http://10.0.2.2:8080/api/` |
| `PROD_BASE_URL` | URL de la API en entorno de producción | *(tu dominio real)* |
 
> **Nota:** Para conectar desde un **dispositivo físico**, sustituye `10.0.2.2` por la IP local de tu máquina donde corre la API.
 
### Instalación y Ejecución
 
1. Clona el repositorio.
2. Crea el archivo `secrets.properties` con las variables indicadas arriba.
3. Abre el proyecto en Android Studio y sincroniza con Gradle para descargar las dependencias definidas en `libs.versions.toml`.
4. Ejecuta la variante `debug` en un emulador o dispositivo físico.
---
 
## Funcionalidades Implementadas
 
- **Autenticación Stateless:** Gestión de tokens JWT con persistencia segura, interceptor 401 global y renovación de sesión con logout automático.
- **Fichaje con Geovallado:** Uso de `FusedLocationProviderClient` para validar la posición del empleado al iniciar o finalizar jornada.
- **Dashboard BFF:** Panel central que sincroniza el estado actual del empleado (fichajes activos y turnos) en una sola petición optimizada al backend.
- **Gestión de Turnos:** Listado y sincronización de los turnos asignados al empleado.
- **Gestión de Ausencias:** Formulario de solicitud con soporte multipart para adjuntar justificantes, y listado de ausencias propias con polling.
- **Perfil de Usuario:** Pantalla de perfil con cambio de contraseña y logout global.
- **Persistencia Offline:** Room Database para consulta de datos sin conexión.
- **Soporte Multilingüe:** Localización completa para Castellano (ES) e Inglés (EN).
- **Modo Oscuro:** Soporte nativo para temas Dark/Light según la configuración del sistema.
---
 
## CI/CD
 
El proyecto dispone de un pipeline de integración continua definido en `.github/workflows/android-ci.yml` que se ejecuta automáticamente en cada push a `main`, `feature/**`, `fix/**` o `refactor/**`, y en cada Pull Request a `main`.
 
El pipeline realiza las siguientes etapas en orden:
 
1. **Configuración del entorno:** Prepara JDK 17 con caché de Gradle para acelerar builds sucesivos.
2. **Generación de secrets:** Inyecta las variables de entorno necesarias (`DEV_BASE_URL`, `PROD_BASE_URL`) para que el proyecto compile correctamente en CI.
3. **Compilación:** Genera el APK de debug con `./gradlew assembleDebug`.
4. **Tests:** Ejecuta los tests unitarios con `./gradlew testDebugUnitTest`.
---
 
## Versionado
 
Este proyecto utiliza **Git tags anotados** para marcar hitos funcionales, siguiendo **Semantic Versioning** (`MAJOR.MINOR.PATCH`):
 
- **MAJOR**: cambios incompatibles o nuevos roles funcionales completos.
- **MINOR**: nuevas funcionalidades compatibles (épicas cerradas).
- **PATCH**: correcciones compatibles sin ruptura de funcionalidad.
### Hitos publicados
 
- **`v0.1.0`** → infraestructura base y autenticación.
  Arquitectura Clean + MVVM lista, Retrofit configurado, Navigation Compose implementado y flujo de login con persistencia de JWT funcional.
- **`v0.5.0`** → motor de fichaje operativo.
  Validación GPS nativa con `FusedLocationProviderClient`, UI reactiva del cronómetro de jornada y comunicación de entrada/salida con el backend.
- **`v0.9.0-beta`** → versión beta actual. *(latest)*
  Incluye persistencia offline con Room, listado de turnos, pantalla de perfil con logout global y formulario de ausencias con soporte multipart.
### Roadmap
 
- **`v1.0.0`** → primera versión estable del rol EMPLEADO. *(en desarrollo)*
  Completará el backlog del empleado con: historial de fichajes personales, listado y sincronización de ausencias con polling, documentación KDoc completa y limpieza de código, además de las mejoras de calidad e infraestructura pendientes.
- **`v2.0.0`** → rol SUPERVISOR completo. *(planificado)*
  Añadirá todas las funcionalidades de gestión de equipo: navegación condicional por rol, cuadrante del departamento, dashboard estadístico, modificación manual de fichajes, validación de ausencias del equipo y asignación de turnos a empleados.
### Criterio de uso
 
Para integración con el backend y despliegue, la referencia será siempre la **última versión estable aprobada**, no necesariamente el último commit de la rama `main`.
 
---
 
## Normativa de Contribución
 
Se aplica la **"Norma de Oro"** del proyecto para mantener la integridad del código:
 
1. **Prohibido hacer commits directos a `main`**.
2. Todo cambio debe realizarse en una rama `feature/`, `fix/` o `refactor/`.
3. Se requiere la apertura de una **Pull Request** y la superación de los **Status Checks** de CI para el merge.
---
 
## Licencia
 
Este proyecto se distribuye bajo la **Licencia MIT** — consulta el archivo [LICENSE](LICENSE) para más detalles.
