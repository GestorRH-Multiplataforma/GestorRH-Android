package com.gestorrh.android.core.network

import com.gestorrh.android.BuildConfig
import com.gestorrh.android.core.security.SessionManager
import com.google.gson.GsonBuilder
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.gestorrh.android.core.network.LocalTimeDeserializer
import java.time.LocalTime
import java.time.ZoneId

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
            .registerTypeAdapter(
                LocalDateTime::class.java,
                JsonSerializer<LocalDateTime> { src, _, _ ->
                    JsonPrimitive(
                        src.atZone(ZoneId.systemDefault())
                            .toOffsetDateTime()
                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    )
                }
            )
            .registerTypeAdapter(LocalDate::class.java, LocalDateDeserializer())
            .registerTypeAdapter(
                LocalDate::class.java,
                JsonSerializer<LocalDate> { src, _, _ ->
                    JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE))
                }
            )
            .registerTypeAdapter(LocalTime::class.java, LocalTimeDeserializer())
            .registerTypeAdapter(
                LocalTime::class.java,
                JsonSerializer<LocalTime> { src, _, _ ->
                    JsonPrimitive(src.format(DateTimeFormatter.ofPattern("HH:mm")))
                }
            )
            .create()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(clienteHttp)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
}
