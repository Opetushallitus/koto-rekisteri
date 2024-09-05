package fi.oph.kitu

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KituApplication

fun main(args: Array<String>) {
    runApplication<KituApplication>(*args)
}
