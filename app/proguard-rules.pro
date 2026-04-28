# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ════════════════════════════════════════════════════════════════
#  GestorRH Android — ProGuard Rules
#  Actualizar este archivo al añadir nuevas librerías o módulos
# ════════════════════════════════════════════════════════════════


# ── Retrofit + OkHttp ────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*


# ── OkHttp Multipart (AusenciaRepositoryImpl) ───────────────────
-keep class okhttp3.RequestBody { *; }
-keep class okhttp3.MediaType { *; }
-keepclassmembers class okhttp3.** {
    public static ** parse(java.lang.String);
}


# ── Gson ─────────────────────────────────────────────────────────
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn sun.misc.**


# ── DTOs de red (serialización JSON) ─────────────────────────────
-keep class com.gestorrh.android.data.network.** { *; }


# ── Room ─────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public static ** getInstance(...);
}
-dontwarn androidx.room.**


# ── Entidades locales Room ───────────────────────────────────────
-keep class com.gestorrh.android.data.local.entity.** { *; }


# ── EncryptedSharedPreferences ───────────────────────────────────
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**


# ── Android KeyStore (SessionManager + MasterKey) ────────────────
-keep class android.security.keystore.** { *; }
-dontwarn android.security.keystore.**


# ── AuthEventBus (singleton con SharedFlow) ──────────────────────
-keep class com.gestorrh.android.core.security.AuthEventBus { *; }


# ── Kotlin ───────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}


# ── Kotlin Coroutines ────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**


# ── WorkManager ──────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keepclassmembers class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn androidx.work.**


# ── Google Play Services (Geolocalización) ───────────────────────
-keep class com.google.android.gms.location.** { *; }
-dontwarn com.google.android.gms.**


# ── Jetpack Compose ──────────────────────────────────────────────
-dontwarn androidx.compose.**


# ── Modelos de dominio y repositorios ───────────────────────────
-keep class com.gestorrh.android.domain.** { *; }


# ── Depuración de stack traces en release ────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile