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
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = (System.getenv("BUILD_NUMBER")?.toIntOrNull()) ?: 1
        versionName = "1.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            val devUrl = secrets.getProperty("DEV_BASE_URL")
                ?: throw GradleException("ERROR ARQUITECTURA: Falta DEV_BASE_URL en secrets.properties")
            buildConfigField("String", "BASE_URL", devUrl)
        }

        create("staging") {
            initWith(getByName("debug"))
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val devUrl = secrets.getProperty("DEV_BASE_URL")
                ?: throw GradleException("ERROR ARQUITECTURA: Falta DEV_BASE_URL en secrets.properties")
            buildConfigField("String", "BASE_URL", devUrl)
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.core.splashscreen)

    // 1. Red (Retrofit + Gson + Interceptor para Logs)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)

    // 2. Navegación en Jetpack Compose
    implementation(libs.androidx.navigation.compose)
    implementation(libs.accompanist.navigation.animation)

    // 3. Seguridad (EncryptedSharedPreferences para el JWT)
    implementation(libs.androidx.security.crypto)

    // 4. Geolocalización (FusedLocationProviderClient)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    // 5. Persistencia local (Room)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // 6. Lifecycle Compose (collectAsStateWithLifecycle)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // 7. WorkManager (sincronización diferida de fichajes offline)
    implementation(libs.androidx.work.runtime.ktx)
}