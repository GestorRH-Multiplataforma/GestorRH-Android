package com.gestorrh.android.core.network

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Enseña a Retrofit (Gson) cómo transformar el String "yyyy-MM-dd" de Spring Boot
 * en un objeto LocalDate de Kotlin.
 */
class LocalDateDeserializer : JsonDeserializer<LocalDate> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDate {
        return LocalDate.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE)
    }
}
