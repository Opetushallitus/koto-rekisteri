package fi.oph.kitu

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class KituApplication

fun main(args: Array<String>) {
    runApplication<KituApplication>(*args)
}
