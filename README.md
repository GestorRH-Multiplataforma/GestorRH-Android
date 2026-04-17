# GestorRH - Cliente Android

Cliente móvil nativo desarrollado para el ecosistema **GestorRH 2.0**. Esta aplicación permite a los empleados gestionar su jornada laboral, consultar turnos y tramitar ausencias mediante una interfaz moderna y reactiva, integrada con el backend centralizado [GestorRH-API](https://github.com/GestorRH-Multiplataforma/GestorRH-API).

![Kotlin](https://img.shields.io/badge/kotlin-v1.9.22-7F52FF?logo=kotlin)
![Android SDK](https://img.shields.io/badge/SDK-34%2B-3DDC84?logo=android)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose)
![License](https://img.shields.io/badge/license-MIT-yellow)
[![Android CI](https://github.com/GestorRH-Multiplataforma/gestorrh-android/actions/workflows/android-ci.yml/badge.svg)](https://github.com/GestorRH-Multiplataforma/gestorrh-android/actions/workflows/android-ci.yml)

## Arquitectura y Pila Tecnológica

El proyecto está construido siguiendo los principios de **Clean Architecture** y el patrón de diseño **MVVM**, garantizando la separación de responsabilidades y la facilidad de testeo.

* **Lenguaje:** Kotlin.
* **UI:** Jetpack Compose con Material Design 3.
* **Inyección de Dependencias:** Hilt (Dagger).
* **Red:** Retrofit 2 & OkHttp 4.
* **Persistencia Local:** Room Database para soporte offline.
* **Navegación:** Compose Navigation con rutas fuertemente tipadas.
* **Concurrencia:** Kotlin Coroutines & Flow.
* **CI/CD:** GitHub Actions para compilación y verificación automática.

## Estructura del Proyecto

Basado en el código fuente, la aplicación se organiza en los siguientes paquetes core:

* **`core`**: Configuraciones transversales como la lógica de localización (`GestorLocalizacion`), interceptores de red (`AuthInterceptor`), gestión de sesión (`SessionManager`) y navegación.
* **`data`**: Implementación de la capa de datos, incluyendo servicios de API (`FichajeApiService`, `AuthApi`), modelos de respuesta/petición y repositorios.
* **`domain`**: Definición de las interfaces de los repositorios y modelos de negocio puros.
* **`ui`**: Capa de presentación que contiene las pantallas (Dashboard, Login, Perfil), sus respectivos `ViewModels` y el tema visual de la app.

## Configuración y Ejecución

### Requisitos Previos
* **Android Studio** (Versión Iguana o superior).
* **JDK 17**.
* **[GestorRH-API](https://github.com/GestorRH-Multiplataforma/GestorRH-API)** en ejecución para el consumo de datos.

### Conexión con el Backend (BFF)
Por defecto, la aplicación está configurada para conectar con la API en un entorno de emulador local:
* **URL Base:** `http://10.0.2.2:8080/api/`.

Para despliegue en dispositivo físico, se debe actualizar la URL en `ApiClient.kt` o mediante perfiles de compilación en `build.gradle.kts`.

### Instalación
1. Clonar el repositorio.
2. Sincronizar el proyecto con Gradle para descargar las dependencias definidas en `libs.versions.toml`.
3. Ejecutar la variante de `debug` en un dispositivo o emulador.

## Funcionalidades Implementadas

* **Autenticación Stateless:** Gestión de tokens JWT con persistencia segura y renovación de sesión.
* **Fichaje con Geovallado:** Uso de `FusedLocationProviderClient` para validar la posición del empleado al iniciar o finalizar jornada.
* **Dashboard BFF:** Panel central que sincroniza el estado actual del empleado (fichajes activos y turnos) en una sola petición optimizada.
* **Soporte Multilingüe:** Localización completa para Castellano (ES) e Inglés (EN).
* **Modo Oscuro:** Soporte nativo para temas Dark/Light según la configuración del sistema.

## Normativa de Contribución

Se aplica la **"Norma de Oro"** del proyecto para mantener la integridad del código:
1. **Prohibido hacer commits directos a `main`**.
2. Todo cambio debe realizarse en una rama `feature/` o `fix/`.
3. Se requiere la apertura de una **Pull Request** y la superación de los **Status Checks** de CI para el merge.

## Licencia

Este proyecto se distribuye bajo la **Licencia MIT**. Puedes consultar el archivo `LICENSE` para más detalles.

***
*Este repositorio es parte del ecosistema multiplataforma desarrollado por [GestorRH-Multiplataforma](https://github.com/GestorRH-Multiplataforma).*
