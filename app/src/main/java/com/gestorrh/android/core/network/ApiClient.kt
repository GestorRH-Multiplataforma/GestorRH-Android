package com.gestorrh.android.core.network

import com.gestorrh.android.BuildConfig
import com.gestorrh.android.core.security.SessionManager
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime

/**
 * Motor central de comunicaciones HTTP de la aplicación.
 * Configura y ensambla el cliente de red aplicando el [AuthInterceptor] para
 * inyección de JWT e intercepción global de 401.
 */
object ApiClient {

    /**
     * Construye una instancia configurada de Retrofit.
     *
     * @param sessionManager Fuente de verdad de la sesión activa, inyectada en [AuthInterceptor].
     * @return Cliente Retrofit listo para consumir los servicios del backend.
     */
    fun crearRetrofit(sessionManager: SessionManager): Retrofit {

        val interceptorLogs = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val clienteHttp = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(sessionManager))
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
