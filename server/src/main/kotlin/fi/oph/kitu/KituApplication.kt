package fi.oph.kitu

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity

@SpringBootApplication
@EnableAsync
@EnableMethodSecurity
class KituApplication

fun main(args: Array<String>) {
    runApplication<KituApplication>(*args)
}
