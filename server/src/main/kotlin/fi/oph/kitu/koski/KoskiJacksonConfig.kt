package fi.oph.kitu.koski

import fi.oph.kitu.koodisto.KoskiKoodiviite
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer
import tools.jackson.databind.ext.javatime.ser.ZonedDateTimeSerializer
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.module.SimpleModule
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Configuration
class KoskiJacksonConfig {
    // @Primary: when jackson-dataformat-xml is on the classpath, Spring Boot
    // auto-configures an `xmlMapper` bean that competes with this one for any
    // generic `ObjectMapper` injection point (e.g. AuditLogger).
    @Bean("koskiObjectMapper")
    @Primary
    fun koskiObjectMapper(): JsonMapper {
        val javaTime =
            SimpleModule()
                .addSerializer(LocalDate::class.java, LocalDateSerializer(DateTimeFormatter.ISO_LOCAL_DATE))
                .addSerializer(
                    LocalDateTime::class.java,
                    LocalDateTimeSerializer(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                ).addSerializer(
                    ZonedDateTime::class.java,
                    ZonedDateTimeSerializer(DateTimeFormatter.ISO_ZONED_DATE_TIME),
                )

        return jacksonMapperBuilder()
            .addModule(KoskiKoodiviite.Companion.KoskiKoodiviiteModule())
            .addModule(javaTime)
            .build()
    }
}
