# GestorRH Android - AI Assistant Instructions

## 1. Stack Tecnológico Base
- **Lenguaje:** Kotlin
- **compileSdk:** 36 (minorApiLevel 1) — **no bajar este valor**
- **minSdk:** 27 / **targetSdk:** 36
- **Compatibilidad Java:** VERSION_11
- **UI:** Jetpack Compose (Material 3) con Single-Activity Architecture (`MainActivity`)
- **Navegación:** `androidx.navigation:navigation-compose:2.7.7`
- **Asincronía:** Coroutines + `StateFlow` / `Flow`
- **Red:** Retrofit 2.9.0 + OkHttp + Gson (`converter-gson:2.9.0`)
- **Inyección de dependencias:** Factories manuales — **NO usar Koin, Hilt ni Dagger**
- **Seguridad:** `EncryptedSharedPreferences` (AES256_GCM) vía `TokenManager`
- **Geolocalización:** Google Play Services Location `21.2.0` + `kotlinx-coroutines-play-services:1.10.2`
- **Gestor de dependencias:** Gradle con `build.gradle.kts` y catálogo `libs.versions.toml`
- **URLs de entorno:** Inyectadas vía `BuildConfig.BASE_URL` desde `secrets.properties` — **nunca hardcodear IPs**

---

## 2. Estructura de Paquetes

```
com.gestorrh.android/
├── core/
│   ├── location/
│   │   └── GestorLocalizacion.kt       # FusedLocationProviderClient, lógica GPS
│   ├── navigation/
│   │   ├── BarraNavegacionInferior.kt  # Bottom navigation bar
│   │   └── RutasDestino.kt             # Definición de rutas de navegación
│   ├── network/
│   │   ├── ApiClient.kt                # Configuración base de Retrofit + Interceptor JWT + LocalDateTimeDeserializer
│   │   └── LocalDateTimeDeserializer.kt
│   └── security/
│       └── TokenManager.kt             # EncryptedSharedPreferences, guardar/leer/borrar JWT
├── data/
│   └── network/                        # DTOs e interfaces Retrofit por entidad
│       ├── autenticacion/
│       │   ├── AuthApi.kt
│       │   └── AuthModels.kt
│       └── fichaje/
│           ├── FichajeApiService.kt
│           ├── FichajeRequests.kt
│           ├── FichajeResponses.kt
│           └── ModalidadTurno.kt       # Enum: PRESENCIAL / TELETRABAJO
├── domain/                             # (en desarrollo — lógica de negocio pura)
└── ui/
    ├── dashboard/
    │   ├── DashboardViewModel.kt
    │   └── PantallaDashboard.kt
    ├── login/
    │   ├── EstadoUiLogin.kt            # data class de estado de UI
    │   ├── LoginViewModel.kt
    │   └── PantallaLogin.kt
    ├── principal/
    │   └── PantallaPrincipal.kt        # Scaffold principal con navegación
    └── theme/
        ├── Color.kt
        ├── Shape.kt
        ├── Theme.kt
        └── Type.kt

res/
├── values/
│   └── strings.xml                     # TODOS los textos visibles aquí
MainActivity.kt                         # Única Activity del proyecto
```

---

## 3. Arquitectura MVVM — Convenciones estrictas

### UI (Jetpack Compose)
- Pantallas completas usan el prefijo `Pantalla` (ej. `PantallaDashboard.kt`)
- El estado se consume exclusivamente desde el ViewModel via `collectAsState()`
- **PROHIBIDO hardcodear strings visibles.** Todo texto usa `stringResource(id = R.string.xxx)`
- La UI es "tonta": solo emite eventos (clics, permisos) al ViewModel y reacciona al estado
- Nuevas features siguen la estructura `ui/<feature>/`: `EstadoUiXxx.kt`, `XxxViewModel.kt`, `PantallaXxx.kt`

### ViewModels
- Exponen un único `StateFlow<EstadoUiXxx>` con un `data class` inmutable
- Mutaciones siempre con `_estadoUi.update { it.copy(...) }`
- Inyectados mediante factories manuales — **NO usar Koin, Hilt ni ningún framework de DI**
- El Factory se declara en el mismo archivo del ViewModel como `companion object`

### Capa de red y DTOs
- Los DTOs en `data/network/<entidad>/` tienen exactamente los mismos nombres y tipos que la API Spring Boot
- Propiedades opcionales marcadas con `?`
- Fechas: `LocalDateTime` de `java.time` — el `LocalDateTimeDeserializer` en `ApiClient` maneja ISO-8601
- El Interceptor en `ApiClient` gestiona la inyección del JWT — **no añadir el token manualmente en los servicios**
- **No incluir el token** en rutas `/auth/**` ni `/api/empresas/registro` — ya está excluido en el Interceptor

---

## 4. Reglas de Negocio Críticas

### Geovallado (espeja la lógica del servidor)
- Turno **PRESENCIAL:** obligatorio solicitar `ACCESS_FINE_LOCATION` y enviar coordenadas reales. El servidor las valida contra el radio de la sede
- Turno **TELETRABAJO:** NO encender el GPS, enviar coordenadas `null`. El servidor lo acepta

### Manejo de errores de la API
- La API Spring Boot responde siempre con un JSON con campo `"message"` en errores 4xx/5xx
- Leer el `errorBody` de la respuesta Retrofit, extraer `"message"` y mostrarlo en un `Snackbar`
- **Nunca mostrar mensajes de error genéricos** cuando la API devuelve uno específico

### Expiración de sesión (401 — pendiente de implementar)
- Cuando la API devuelve HTTP 401: limpiar el token en `TokenManager` y redirigir al login
- **No implementar aún** — es una funcionalidad futura planificada. Si se detecta un 401 sin manejo, añadir un TODO con el ticket correspondiente

---

## 5. Seguridad y Configuración de Entornos

- `secrets.properties` contiene `DEV_BASE_URL` y `PROD_BASE_URL` — **nunca commitear este fichero**
- `BuildConfig.BASE_URL` es la única forma válida de acceder a la URL base en el código
- El `build.gradle.kts` lanza `GradleException` si falta alguna URL — es un Fail-Fast intencionado, **no es un bug**
- `TokenManager` usa `EncryptedSharedPreferences` (AES256_GCM) — no guardar el JWT en ningún otro sitio

---

## 6. Antipatrones prohibidos

1. **Context Leak:** nunca pasar `Activity` o `Context` como parámetro de retención a un ViewModel. Si necesita contexto inicial, pasar `applicationContext` a través del Factory
2. **Hardcoded strings:** ningún texto visible en Composables — siempre `stringResource()`
3. **Doble envío de peticiones:** usar `estaCargando = true` en el estado de UI para bloquear la acción mientras hay una petición en curso (ej. doble clic en fichar)
4. **PII en logs:** no filtrar tokens, emails ni datos personales en logs de debug
5. **Lógica de negocio en la UI:** toda decisión va en el ViewModel o en la capa de dominio
6. **Frameworks de DI:** no introducir Koin, Hilt, Dagger ni ningún contenedor de inyección de dependencias — usar exclusivamente factories manuales

---

## 7. Comandos Gradle

```bash
# Compilar el proyecto
./gradlew assembleDebug

# Ejecutar tests unitarios
./gradlew test

# Ejecutar tests de instrumentación (requiere emulador/dispositivo)
./gradlew connectedAndroidTest

# Compilar sin tests
./gradlew assembleDebug -x test

# Build de release
./gradlew assembleRelease

# Limpiar build
./gradlew clean
```

---

## 8. Flujo de Trabajo Git

Mismo flujo que la API — ambos repos deben ser consistentes:

- **Ramas:** Git Flow simplificado. `main` es producción protegida — nunca push directo
  - Funcionalidades: `feature/P0-XX-descripcion`
  - Parches críticos: `hotfix/descripcion-corta`
  - Configuración: `chore/descripcion`
- **Commits:** Conventional Commits obligatorio (`feat:`, `fix:`, `chore:`, `docs:`). Referenciar la issue al final (ej. `feat: pantalla de fichaje con geovallado (#45)`)
- **Pull Requests:** La descripción de toda PR que resuelva una issue debe incluir obligatoriamente `Closes #XX` (siendo `XX` el número de la issue) para que GitHub la cierre automáticamente al hacer merge. Ejemplo:
  ```
  Closes #45
  ```
  Si la PR resuelve varias issues: `Closes #45, Closes #46`
- **Semantic Versioning:** `versionName` en `build.gradle.kts` solo se incrementa en Release Train, no por cada PR

### Limpieza de ramas locales
Después de que una PR sea mergeada en `main`, eliminar inmediatamente la rama local correspondiente. Al final de cada jornada de trabajo el repositorio local solo debe contener `main` y, si procede, la rama en la que se esté trabajando actualmente:

```bash
git checkout main
git pull origin main
git branch -d nombre-de-la-rama
```

La rama remota en GitHub se mantiene — solo se elimina la copia local.

---

## 9. Autoría del código

Todo el código, comentarios, mensajes de commit, descripciones de PR y cualquier texto generado en el contexto de este proyecto debe redactarse en primera persona y reflejar autoría propia.

**Queda estrictamente prohibido** incluir cualquier referencia, mención directa o indirecta, o cualquier indicación de que el código o texto ha sido generado o asistido por herramientas externas. Esto incluye sin excepción:

- Comentarios en el código fuente
- Mensajes de commit
- Títulos y descripciones de Pull Request
- Documentación KDoc
- El propio `CLAUDE.md`

---

## 10. Al implementar una nueva feature

1. Crear `data/network/<entidad>/` con los DTOs (Requests + Responses) y la interfaz Retrofit
2. Crear `data/repository/<entidad>/` con la interfaz del Repository (en `domain`) y su implementación (en `data`)
3. Crear `ui/<feature>/EstadoUiXxx.kt` con el `data class` de estado
4. Crear `ui/<feature>/XxxViewModel.kt` con el `StateFlow`, la lógica y el Factory manual como `companion object`
5. Crear `ui/<feature>/PantallaXxx.kt` — sin strings hardcodeados, sin lógica de negocio
6. Añadir la ruta en `RutasDestino.kt` y el destino en la navegación
7. Añadir los strings nuevos en `res/values/strings.xml` y `res/values-en/strings.xml`
8. Verificar con `./gradlew test` antes de abrir el PR
9. Rama: `feature/PX-XX-descripcion` — commit con Conventional Commits referenciando la issue
10. Tras el merge de la PR: ejecutar limpieza de rama local (ver sección 8)
