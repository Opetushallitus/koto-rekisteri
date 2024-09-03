package fi.oph.koto

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KotoApplication

fun main(args: Array<String>) {
	runApplication<KotoApplication>(*args)
}
