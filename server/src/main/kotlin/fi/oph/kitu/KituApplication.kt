package fi.oph.kitu

import fi.oph.kitu.logging.add
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationEvent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ApplicationContextEvent
import org.springframework.context.event.GenericApplicationListener
import org.springframework.scheduling.annotation.EnableAsync
import java.util.function.Consumer

@SpringBootApplication
@EnableAsync
class KituApplication

fun main(args: Array<String>) {
    runApplication<KituApplication>(*args)
}

@Configuration
class LifecycleListener {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun start() =
        forEventType<ApplicationContextEvent> { event ->
            logger.atInfo().add(
                "event" to event.javaClass.simpleName,
                "timestamp" to event.timestamp,
            )
        }
}

inline fun <reified E : ApplicationEvent> forEventType(consumer: Consumer<E>): GenericApplicationListener =
    GenericApplicationListener.forEventType<E>(E::class.java, consumer)
