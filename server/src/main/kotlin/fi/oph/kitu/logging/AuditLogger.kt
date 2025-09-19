package fi.oph.kitu.logging

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.Oid
import fi.oph.kitu.auth.CasUserDetails
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.semconv.incubating.ServiceIncubatingAttributes
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger

const val AUDIT_LOGGER_NAME = "auditLogger"

@Service
class AuditLogger(
    @Qualifier("applicationTaskExecutor")
    private val taskExecutor: AsyncTaskExecutor,
    private val environment: Environment,
    private val objectMapper: ObjectMapper,
    private val resource: Resource,
) {
    @Value("\${kitu.appUrl}")
    lateinit var appUrl: String

    private val slf4jLogger = LoggerFactory.getLogger(AUDIT_LOGGER_NAME)

    private val currentZone = ZoneId.of("Europe/Helsinki")
    private val clock = Clock.system(currentZone)
    private val logSeq = AtomicInteger(0)
    private val bootTime = Instant.now(clock)
    private val instanceId = resource.getAttribute(ServiceIncubatingAttributes.SERVICE_INSTANCE_ID) ?: "not set"

    /**
     * Logs events.
     *
     * Note: the logged events with this method will be passed to an audit log integration on a lambda.
     */
    fun log(
        operation: KituAuditLogOperation,
        oppijaHenkiloOid: Oid,
    ) {
        AuditContext.get().forEach { context ->
            slf4jLogger.info(
                objectMapper.writeValueAsString(
                    KituAuditLogMessage(
                        version = 1,
                        logSeq = logSeq.getAndIncrement(),
                        bootTime = bootTime,
                        type = "log",
                        environment = environment.getRequiredProperty("kitu.env.name"),
                        hostname = instanceId,
                        timestamp = Instant.now(),
                        serviceName = "kitu",
                        applicationType = "backend",
                        user = KituAuditLogMessage.User(context.userOid),
                        target = KituAuditLogMessage.Target(oppijaHenkiloOid),
                        organizationOid = context.opetushallitusOrganisaatioOid,
                        operation = operation,
                    ),
                ),
            )
        }
    }

    fun <E> logAllInternalOnly(
        message: String,
        entities: Iterable<E>,
        properties: (E) -> Array<Pair<String, Any?>>,
    ) {
        val userId =
            (SecurityContextHolder.getContext().authentication?.principal as? CasUserDetails)?.oid
        taskExecutor.execute {
            for (entity in entities) {
                val props = properties(entity) + ("principal.oid" to userId)
                slf4jLogger
                    .atInfo()
                    .add(*props)
                    .log(message)
            }
        }
    }
}
