package com.gestorrh.android.core.network

import com.gestorrh.android.BuildConfig
import com.gestorrh.android.core.security.TokenManager
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime

/**
 * Motor central de comunicaciones HTTP de la aplicación.
 * Configura y ensambla el cliente de red inyectando automáticamente las políticas
 * de seguridad y el registro de eventos (logs).
 */
object ApiClient {

    /**
     * Construye una instancia configurada de Retrofit.
     * Aplica dinámicamente el token JWT de sesión a todas las peticiones salientes
     * y oculta el tráfico de red en entornos de producción por seguridad.
     *
     * @param gestorToken Dependencia para acceder a la caja fuerte de credenciales.
     * @return Cliente Retrofit listo para consumir los servicios del backend.
     */
    fun crearRetrofit(gestorToken: TokenManager): Retrofit {

        val interceptorLogs = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val interceptorAutenticacion = Interceptor { cadenaCarga ->
            val peticionOriginal = cadenaCarga.request()
            val ruta = peticionOriginal.url.encodedPath

            if (ruta.contains("/auth/login-empresa") ||
                ruta.contains("/auth/login-empleado") ||
                ruta.contains("/api/empresas/registro")) {
                return@Interceptor cadenaCarga.proceed(peticionOriginal)
            }

            val token = gestorToken.obtenerToken()
            val constructorPeticion = peticionOriginal.newBuilder()

            if (!token.isNullOrEmpty()) {
                constructorPeticion.addHeader("Authorization", "Bearer $token")
            }

            cadenaCarga.proceed(constructorPeticion.build())
        }

        val clienteHttp = OkHttpClient.Builder()
            .addInterceptor(interceptorAutenticacion)
            .addInterceptor(interceptorLogs)
            .build()

        val gson = GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeDeserializer())
            .create()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(clienteHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}