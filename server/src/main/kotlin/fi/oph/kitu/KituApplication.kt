package fi.oph.kitu

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KituApplication

fun main(args: Array<String>) {
    println(foobar())
    foobar()
    runApplication<KituApplication>(*args)
}

// Testing detekt

fun foobar(): Int = 42 * 2
