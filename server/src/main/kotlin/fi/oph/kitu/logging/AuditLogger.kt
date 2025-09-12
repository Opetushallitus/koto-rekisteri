package fi.oph.kitu.logging

import fi.oph.kitu.auth.CasUserDetails
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

const val AUDIT_LOGGER_NAME = "auditLogger"

@Service
class AuditLogger(
    @Qualifier("applicationTaskExecutor")
    private val taskExecutor: AsyncTaskExecutor,
) {
    private val slf4jLogger = LoggerFactory.getLogger(AUDIT_LOGGER_NAME)
    // private val audit: Audit = Audit(this, "kitu", ApplicationType.BACKEND)

    fun simpleLog(msg: String?) {
        slf4jLogger.info(msg)
    }

    fun logWithProperties(
        message: String,
        vararg properties: Pair<String, Any?>,
    ) {
        slf4jLogger
            .atInfo()
            .add(*properties)
            .log(message)
    }

    // Called by other classes (below)

    /**
     * Logs events.
     *
     * Note: the logged events with this method will be passed to an audit log integration on a lambda.
     */
    fun log(
        operation: KituAuditLogOperation, // esim: VktSuoritusViewed
        target: Iterable<Pair<KituAuditLogMessageField, String>>,
        // (OPPIJA_OPPIJANUMERO, 1.2.246.562.24.98167097342)
    ) = logAndSendToKoski(AuditContext.get(), operation, target)

    /**
     * Logs events.
     *
     * Note: the logged events with this method will be passed to an audit log integration on a lambda.
     */
    fun logAndSendToKoski(
        context: AuditContext,
        operation: KituAuditLogOperation,
        target: Iterable<Pair<KituAuditLogMessageField, String>>,
    ) {
        TODO()
        // val user = context.user()

        /*
        val targetBuilder = AuditTarget.Builder()

        // NOTE: Use OID of Opetushallitus for every user of this application.
        // It is done, because at the time (2025-09-12),
        // only users of Opetushallitus are expected to use this application.
        // If this is no longer the case,
        // you need to implement how to fetch correct organization OID for the logged in user
        targetBuilder.setField("organizationOid", context.opetushallitusOrganisaatioOid.toString())

        for ((key, value) in target) {
            targetBuilder.setField(key.key, value)
        }

        val json =
            """
            {
                'version': 1,
                'logSeq': $log_data.log_seq,
                'bootTime': $boot_time_str,
                'type': 'dataAccess',
                'environment': $environment,
                'hostname': $hostname,
                'timestamp': $data_access_log.timestamp.isoformat(),
                'serviceName': 'kitu',
                'applicationType': 'backend',
                'user': {
                    'oid': $user_oid
                },
                'target': {
                    'oppijaHenkiloOid': $data_access_log.henkilo_oid
                },
                'organizationOid': $data_access_log.organisaatio.organisaatio_oid,
                'operation': $data_access_log.access_type
            }
            """.trimIndent()
*/

        // audit.log(user, operation, targetBuilder.build(), changes)
        //  slf4jLogger.info(msg)
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
