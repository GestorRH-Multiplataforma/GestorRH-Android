import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val secretsFile = rootProject.file("secrets.properties")
val secrets = Properties()
if (secretsFile.exists()) {
    secrets.load(FileInputStream(secretsFile))
}

android {
    namespace = "com.gestorrh.android"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.gestorrh.android"
        minSdk = 27
        targetSdk = 36
        versionCode = (System.getenv("BUILD_NUMBER")?.toIntOrNull()) ?: 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Entorno DEV (Pruebas locales)
            val devUrl = secrets.getProperty("DEV_BASE_URL")
                ?: throw GradleException("ERROR ARQUITECTURA: Falta DEV_BASE_URL en secrets.properties")
            buildConfigField("String", "BASE_URL", devUrl)
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Entorno PROD (Despliegue final)
            val prodUrl = secrets.getProperty("PROD_BASE_URL")
                ?: throw GradleException("ERROR ARQUITECTURA: Falta PROD_BASE_URL en secrets.properties")
            buildConfigField("String", "BASE_URL", prodUrl)
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        warningsAsErrors = true
        abortOnError = true
        disable += listOf(
            "GradleDependency",
            "NewerVersionAvailable",
            "AndroidGradlePluginVersion",
            "UseTomlInstead",
            "IconLauncherShape",
            "IconLocation",
            "PluralsCandidate",
            "Typos",
            "StaticFieldLeak"
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // 1. Red (Retrofit + Gson + Interceptor para Logs)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // 2. Navegación en Jetpack Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.google.accompanist:accompanist-navigation-animation:0.34.0")

    // 3. Seguridad (EncryptedSharedPreferences para el JWT)
    implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")

    // 4. Geolocalización (FusedLocationProviderClient)
    implementation("com.google.android.gms:play-services-location:21.2.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // 5. Persistencia local (Room)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // 6. Lifecycle Compose (collectAsStateWithLifecycle)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // 7. WorkManager (sincronización diferida de fichajes offline)
    implementation(libs.androidx.work.runtime.ktx)
}