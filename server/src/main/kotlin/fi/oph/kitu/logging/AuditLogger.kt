package fi.oph.kitu.logging

import fi.oph.kitu.auth.CasUserDetails
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class AuditLogger(
    @Qualifier("applicationTaskExecutor")
    private val taskExecutor: AsyncTaskExecutor,
) {
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

    fun <E> logAll(
        message: String,
        entities: Iterable<E>,
        properties: (E) -> Array<Pair<String, Any?>>,
    ) {
        val userId =
            (SecurityContextHolder.getContext().authentication?.principal as? CasUserDetails)?.oid
        taskExecutor.execute {
            for (entity in entities) {
                log(message, *properties(entity) + ("principal.oid" to userId))
            }
        }
    }
}
