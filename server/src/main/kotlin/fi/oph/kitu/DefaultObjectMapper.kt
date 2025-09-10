package fi.oph.kitu

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Yleiskäyttöinen json-mapper, jolle on konffattu:
 *
 *      - Tuki ISO-aikaleimoille
 *      - Tuki Kotlin-tietotyypeille
 *      - Nätti JSON-tulostus
 *      - Ei välitä yllättävistä propertyista
 */
val defaultObjectMapper by lazy {
    val mapper = ObjectMapper()

    val javaTime =
        JavaTimeModule()
            .addSerializer(LocalDate::class.java, LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE))
            .addSerializer(
                LocalDateTime::class.java,
                LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            ).addSerializer(
                ZonedDateTime::class.java,
                ZonedDateTimeSerializer(DateTimeFormatter.ISO_ZONED_DATE_TIME),
            )

    mapper.registerKotlinModule()
    mapper.registerModule(javaTime)
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    mapper.enable(SerializationFeature.INDENT_OUTPUT)

    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    mapper
}
