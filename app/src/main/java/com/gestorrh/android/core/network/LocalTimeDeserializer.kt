package com.gestorrh.android.core.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Enseña a Retrofit (Gson) cómo transformar el String "HH:mm" de Spring Boot
 * en un objeto LocalTime de Kotlin.
 */
class LocalTimeDeserializer : JsonDeserializer<LocalTime> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): LocalTime {
        return LocalTime.parse(json.asString, DateTimeFormatter.ofPattern("HH:mm"))
    }
}