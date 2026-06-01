package com.gestorrh.android.core.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Enseña a Retrofit (Gson) cómo transformar el String ISO-8601 con offset de la API
 * en un objeto LocalDateTime en la zona horaria local del dispositivo.
 *
 * La API devuelve timestamps con offset explícito (ej: 2026-05-18T17:35:00Z).
 * El deserializador convierte ese instante al equivalente en hora local del dispositivo,
 * de forma que el resto del código puede seguir trabajando con LocalDateTime sin cambios.
 */
class LocalDateTimeDeserializer : JsonDeserializer<LocalDateTime> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LocalDateTime {
        return OffsetDateTime.parse(json.asString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
    }
}