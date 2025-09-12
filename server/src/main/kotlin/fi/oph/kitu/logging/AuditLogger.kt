package fi.oph.kitu.logging

import fi.oph.kitu.Oid
import fi.oph.kitu.auth.CasUserDetails
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
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger

const val AUDIT_LOGGER_NAME = "auditLogger"

@Service
class AuditLogger(
    @Qualifier("applicationTaskExecutor")
    private val taskExecutor: AsyncTaskExecutor,
    private val environment: Environment,
) {
    @Value("\${kitu.appUrl}")
    lateinit var appUrl: String

    private val slf4jLogger = LoggerFactory.getLogger(AUDIT_LOGGER_NAME)

    private val currentZone = ZoneId.of("Europe/Helsinki")
    private val clock = Clock.system(currentZone)
    private val logSeq = AtomicInteger(0)
    private val bootTime = Instant.now(clock)
    private val fmt =
        DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX") // same pattern as SDF
            .withZone(currentZone)

    /**
     * Logs events.
     *
     * Note: the logged events with this method will be passed to an audit log integration on a lambda.
     */
    fun log(
        operation: KituAuditLogOperation,
        oppijaHenkiloOid: String,
    ) {
        val context = AuditContext.get()
        val type = "log"
        val timestamp = fmt.format(Instant.now(clock))

        val json =
            // TODO: Refactor to generate the json with fasterxml.jackson
            """
            {
                "version": 1,
                "logSeq": ${logSeq.getAndIncrement()},
                "bootTime": "${fmt.format(bootTime)}",
                "type": "$type",
                "environment": "${environment.activeProfiles.first()}",
                "hostname": "$appUrl",
                "timestamp": "$timestamp",
                "serviceName": "kitu",
                "applicationType": "backend",
                "user": {"oid": "${context.userOid}"},
                "target": {"${KitAuditLogMessageField.OppijaHenkiloOid}": "$oppijaHenkiloOid"},
                "organizationOid": "${context.opetushallitusOrganisaatioOid}",
                "operation": "${operation.name}"
            }
            """.trimIndent()
        slf4jLogger.info(json)
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
