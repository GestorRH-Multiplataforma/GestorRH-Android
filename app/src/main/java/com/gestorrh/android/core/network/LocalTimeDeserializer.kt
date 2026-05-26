package com.gestorrh.android.core.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

/**
 * Enseña a Retrofit (Gson) cómo transformar Strings de hora de Spring Boot
 * en un objeto LocalTime de Kotlin. Acepta tanto "HH:mm" como "HH:mm:ss"
 * para ser tolerante con distintas configuraciones del servidor.
 */
class LocalTimeDeserializer : JsonDeserializer<LocalTime> {

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LocalTime {
        return LocalTime.parse(json.asString, FORMATTER)
    }

    companion object {
        private val FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
            .appendPattern("HH:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            .toFormatter()
    }
}