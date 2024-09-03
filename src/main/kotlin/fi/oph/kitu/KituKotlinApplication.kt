package fi.oph.kitu

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KituKotlinApplication

fun main(args: Array<String>) {
    runApplication<KituKotlinApplication>(*args)
}
