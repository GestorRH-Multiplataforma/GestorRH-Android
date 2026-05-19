# GestorRH - Cliente Android
 
[![Android CI](https://github.com/GestorRH-Multiplataforma/gestorrh-android/actions/workflows/android-ci.yml/badge.svg)](https://github.com/GestorRH-Multiplataforma/gestorrh-android/actions/workflows/android-ci.yml)
[![Version](https://img.shields.io/badge/version-v1.0.2--stable-brightgreen)](https://github.com/GestorRH-Multiplataforma/gestorrh-android/releases/tag/v1.0.2)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Android SDK](https://img.shields.io/badge/SDK-34%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![Download](https://img.shields.io/badge/Descargar-APK-3DDC84?logo=android&logoColor=white)](https://gestorrh-multiplataforma.github.io/github.io/)
 
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
- **Seguridad:** EncryptedSharedPreferences + ProGuard/R8
- **Sincronización offline:** WorkManager
- **Geolocalización:** FusedLocationProviderClient (Play Services)
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
├── MainActivity.kt
├── GestorRhApplication.kt       
│
├── core/
│   ├── archivos/
│   │   └── GestorArchivosJustificante.kt  
│   ├── location/
│   │   └── GestorLocalizacion.kt
│   ├── navigation/
│   │   ├── BarraNavegacionInferior.kt
│   │   └── RutasDestino.kt
│   ├── network/
│   │   ├── ApiClient.kt
│   │   ├── AuthInterceptor.kt
│   │   ├── ConectividadUtils.kt           
│   │   ├── LocalDateDeserializer.kt
│   │   └── LocalDateTimeDeserializer.kt
│   ├── onboarding/
│   │   └── OnboardingManager.kt           
│   ├── security/
│   │   ├── AuthEventBus.kt
│   │   └── SessionManager.kt
│   └── ui/
│       └── MensajeUi.kt
│
├── data/
│   ├── local/
│   │   ├── GestorRhDatabase.kt
│   │   ├── dao/
│   │   │   ├── AsignacionDao.kt
│   │   │   └── FichajePendienteDao.kt    
│   │   ├── entity/
│   │   │   ├── AsignacionEntity.kt
│   │   │   └── FichajePendienteEntity.kt  
│   │   └── mapper/
│   │       └── AsignacionMapper.kt
│   ├── network/
│   │   ├── asignacion/
│   │   ├── ausencia/
│   │   ├── autenticacion/
│   │   ├── empleado/
│   │   └── fichaje/
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   ├── FichajeRepository.kt
│   │   ├── PerfilRepository.kt
│   │   ├── asignacion/
│   │   │   └── AsignacionRepositoryImpl.kt
│   │   └── ausencia/
│   │       └── AusenciaRepositoryImpl.kt
│   └── sync/                              
│       ├── FichajeSyncManager.kt
│       └── SyncFichajeWorker.kt
│
├── domain/
│   ├── repository/
│   │   ├── IAsignacionRepository.kt
│   │   ├── IAusenciaRepository.kt
│   │   ├── IAuthRepository.kt
│   │   ├── IFichajeRepository.kt
│   │   ├── IPerfilRepository.kt
│   │   └── ResultadoSincronizacion.kt
│   └── usecase/
│       ├── ausencia/
│       │   └── SolicitarAusenciaUseCase.kt
│       └── fichaje/                      
│           ├── GuardarFichajePendienteUseCase.kt
│           └── ObtenerHistorialFichajesUseCase.kt
│
└── ui/
    ├── ausencia/
    │   ├── AusenciaUtils.kt               
    │   ├── EstadoUiMisAusencias.kt       
    │   ├── EstadoUiSolicitudAusencia.kt
    │   ├── MisAusenciasViewModel.kt       
    │   ├── PantallaMisAusencias.kt        
    │   ├── PantallaSolicitudAusencia.kt
    │   └── SolicitudAusenciaViewModel.kt
    ├── dashboard/
    │   ├── DashboardViewModel.kt
    │   └── PantallaDashboard.kt
    ├── historial/                         
    │   ├── EstadoUiHistorialFichajes.kt
    │   ├── HistorialFichajesViewModel.kt
    │   └── PantallaHistorialFichajes.kt
    ├── login/
    │   ├── EstadoUiLogin.kt
    │   ├── LoginViewModel.kt
    │   └── PantallaLogin.kt
    ├── onboarding.kt                      
    ├── perfil/
    │   ├── EstadoUiPerfil.kt
    │   ├── PantallaPerfil.kt
    │   └── PerfilViewModel.kt
    ├── principal/
    │   └── PantallaPrincipal.kt
    ├── theme/
    │   ├── Color.kt
    │   ├── Shape.kt
    │   ├── Theme.kt
    │   └── Type.kt
    └── turnos/
        ├── EstadoUiMisTurnos.kt
        ├── MisTurnosViewModel.kt
        └── PantallaMisTurnos.kt
```
 
---
 
## Configuración del Entorno
 
### Variables de Configuración
 
La aplicación utiliza un archivo `secrets.properties` en la raíz del proyecto para gestionar las URLs del backend según el entorno. Este archivo **no se sube al repositorio** (incluido en `.gitignore`) y se inyecta en CI mediante
GitHub Secrets.
 
Crea el archivo manualmente con el siguiente contenido:
 
```properties
DEV_BASE_URL="http://10.0.2.2:8080/api/"
PROD_BASE_URL="https://tu-dominio.com/api/"
```
 
| Variable | Descripción | Valor por defecto (emulador) |
|---|---|---|
| `DEV_BASE_URL` | URL de la API en entorno de desarrollo | `http://10.0.2.2:8080/api/` |
| `PROD_BASE_URL` | URL de la API en entorno de producción | *(tu dominio real)* |

> **Nota para CI:** La variable `PROD_BASE_URL` se gestiona como
> GitHub Secret en el repositorio y se inyecta automáticamente
> en el pipeline. No es necesario configurarla manualmente para
> que el CI compile correctamente.

> **Nota para dispositivo físico:** Para conectar desde un
> dispositivo físico sustituye `10.0.2.2` por la IP local de
> tu máquina donde corre la API.
 
### Instalación y Ejecución
 
1. Clona el repositorio.
2. Crea el archivo `secrets.properties` con las variables indicadas arriba.
3. Abre el proyecto en Android Studio y sincroniza con Gradle para descargar las dependencias definidas en `libs.versions.toml`.
4. Ejecuta la variante `debug` en un emulador o dispositivo físico.

> **Descarga directa:** Puedes descargar el APK de la última versión estable
> desde la [página de descarga](https://gestorrh-multiplataforma.github.io/github.io/) o directamente desde
> los [releases del repositorio](https://github.com/GestorRH-Multiplataforma/gestorrh-android/releases/tag/v1.0.0).
---

## Funcionalidades Implementadas

- **Autenticación Stateless:** Gestión de tokens JWT con persistencia segura,
  interceptor 401 global y renovación de sesión con logout automático.
- **Onboarding:** Pantalla de bienvenida con HorizontalPager que se muestra
  únicamente en el primer arranque.
- **Fichaje con Geovallado:** Uso de `FusedLocationProviderClient` para validar
  la posición del empleado al iniciar o finalizar jornada.
- **Dashboard BFF:** Panel central que sincroniza el estado actual del empleado
  en una sola petición optimizada al backend, con widget de próximo turno y
  próxima ausencia.
- **Gestión de Turnos:** Listado y sincronización offline-first de los turnos
  asignados, con vista lista y vista calendario.
- **Gestión de Ausencias:** Formulario de solicitud con soporte multipart para
  adjuntar justificantes, listado propio con polling automático y flujo completo
  de edición y cancelación.
- **Historial de Fichajes:** Consulta del historial personal con filtro por
  rango de fechas.
- **Perfil de Usuario:** Pantalla de perfil con cambio de contraseña y logout
  global.
- **Persistencia Offline:** Room Database para consulta de datos sin conexión y
  cola de fichajes pendientes sincronizada por WorkManager.
- **Soporte Multilingüe:** Localización completa para Castellano (ES) e
  Inglés (EN).
- **Modo Oscuro:** Soporte nativo para temas Dark/Light según la configuración
  del sistema.
- **Minificación y Ofuscación:** ProGuard/R8 activado en release para reducir
  el tamaño del APK y proteger el código.
---

## CI/CD

El proyecto dispone de un pipeline de integración continua definido en
`.github/workflows/android-ci.yml` que se ejecuta automáticamente en cada
push a `main`, `feature/**`, `fix/**` o `refactor/**`, y en cada Pull Request
a `main`.

El pipeline realiza las siguientes etapas en orden:

1. **Configuración del entorno:** Prepara JDK 17 con caché de Gradle.
2. **Generación de secrets:** Inyecta `DEV_BASE_URL` y `PROD_BASE_URL`
   desde GitHub Secrets para que el proyecto compile correctamente en CI.
3. **Compilación Debug:** Genera el APK de debug con `./gradlew assembleDebug`.
4. **Tests:** Ejecuta los tests unitarios con `./gradlew testDebugUnitTest`.
5. **Compilación Release:** Valida que el APK de release compila correctamente
   con `./gradlew assembleRelease`.
---
 
## Versionado
 
Este proyecto utiliza **Git tags anotados** para marcar hitos funcionales, siguiendo **Semantic Versioning** (`MAJOR.MINOR.PATCH`):
 
- **MAJOR**: cambios incompatibles o nuevos roles funcionales completos.
- **MINOR**: nuevas funcionalidades compatibles (épicas cerradas).
- **PATCH**: correcciones compatibles sin ruptura de funcionalidad.
### Hitos publicados

- **`v0.1.0`** → infraestructura base y autenticación.
  Arquitectura Clean + MVVM lista, Retrofit configurado, Navigation Compose
  implementado y flujo de login con persistencia de JWT funcional.

- **`v0.5.0`** → motor de fichaje operativo.
  Validación GPS nativa con `FusedLocationProviderClient`, UI reactiva del
  cronómetro de jornada y comunicación de entrada/salida con el backend.

- **`v0.9.0-beta`** → versión beta.
  Incluye persistencia offline con Room, listado de turnos, pantalla de perfil
  con logout global y formulario de ausencias con soporte multipart.

- **`v1.0.0`** → primera versión estable del rol EMPLEADO.
  Historial de fichajes personales, listado y sincronización de ausencias con
  polling, minificación y ofuscación del APK de release activadas, corrección
  del flujo de logout con limpieza de caché, onboarding y soporte multilingüe
  completo (ES/EN).
- **`v1.0.1`** → corrección de conectividad con producción. 
    Corregido el prefijo del endpoint de autenticación para ser consistente con la `PROD_BASE_URL` de producción, resolviendo el fallo de login en dispositivos físicos.
- **`v1.0.2`** → corrección de eliminación de justificante en ausencias. *(latest)*
  Corregido el flujo de edición de ausencias que impedía eliminar un justificante ya adjunto, centralizando el cálculo del flag en el ViewModel y eliminando el recálculo redundante en el caso de uso.
### Roadmap

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
