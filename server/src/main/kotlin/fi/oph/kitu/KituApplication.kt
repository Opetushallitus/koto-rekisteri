package fi.oph.kitu

import fi.oph.kitu.util.TimeService
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import java.util.TimeZone

@SpringBootApplication
@EnableAsync
@EnableMethodSecurity
@ConfigurationPropertiesScan
@EnableRetry
class KituApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone(TimeService.zoneId))
    runApplication<KituApplication>(*args)
}
