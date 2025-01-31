package fi.oph.kitu.logging

import org.slf4j.Logger
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class AuditLogger {
    private val logger: Logger = Logging.auditLogger()

    fun log(
        message: String,
        vararg properties: Pair<String, Any?>,
    ) {
        logger
            .atInfo()
            .add(*properties)
            .log(message)
    }

    @Async
    fun <E> logAll(
        message: String,
        entities: Iterable<E>,
        properties: (E) -> Array<Pair<String, Any?>>,
    ) {
        for (entity in entities) {
            log(message, *properties(entity))
        }
    }
}
