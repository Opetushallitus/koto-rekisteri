package fi.oph.kitu.logging

import fi.oph.kitu.Oid
import fi.oph.kitu.auth.CasUserDetails
import fi.vm.sade.auditlog.ApplicationType
import fi.vm.sade.auditlog.Audit
import fi.vm.sade.auditlog.Changes
import fi.vm.sade.auditlog.Logger
import fi.vm.sade.auditlog.Operation
import fi.vm.sade.auditlog.User
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import java.net.InetAddress
import fi.vm.sade.auditlog.Target as AuditTarget
import org.slf4j.Logger as SLogger

const val AUDIT_LOGGER_NAME = "auditLogger"

@Service
class AuditLogger(
    @Qualifier("applicationTaskExecutor")
    private val taskExecutor: AsyncTaskExecutor,
) : Logger {
    private val logger = AuditLoggerImpl(LoggerFactory.getLogger(AUDIT_LOGGER_NAME))
    private val audit: Audit = Audit(logger, "kitu", ApplicationType.BACKEND)

    override fun log(msg: String?) {
        logger.log(msg)
    }

    fun log(
        message: String,
        vararg properties: Pair<String, Any?>,
    ) {
        logger
            .logger
            .atInfo()
            .add(*properties)
            .log(message)
    }

    fun log(
        context: AuditContext,
        operation: KituAuditLogOperation,
        target: Iterable<Pair<KituAuditLogMessageField, String>>,
        changes: Changes = Changes.EMPTY,
    ) {
        val user = context.user()

        val targetBuilder = AuditTarget.Builder()
        for ((key, value) in target) {
            targetBuilder.setField(key.key, value)
        }

        audit.log(user, operation, targetBuilder.build(), changes)
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

    fun <E> logAll(
        context: AuditContext,
        operation: KituAuditLogOperation,
        entities: Iterable<E>,
        changes: Changes = Changes.EMPTY,
        properties: (E) -> Array<Pair<KituAuditLogMessageField, Any?>>,
    ) {
        taskExecutor.execute {
            for (entity in entities) {
                val props = properties(entity).map { (key, value) -> key to value.toString() }
                log(context, operation, props, changes)
            }
        }
    }
}

data class AuditContext(
    val userOid: Oid,
    val userAgent: String,
    val ip: InetAddress,
    val session: String,
) {
    fun user(): User = User(userOid.unwrap(), ip, session, userAgent)
}

fun HttpServletRequest.toAuditContext(): AuditContext {
    val userDetails = SecurityContextHolder.getContext().authentication?.principal as? CasUserDetails
    val userOid = Oid.parse(userDetails?.oid).getOrThrow()
    val userAgent = this.getHeader("user-agent")
    val ip = InetAddress.getByName(this.remoteAddr)
    val session = this.session.id

    return AuditContext(userOid, userAgent, ip, session)
}

enum class KituAuditLogOperation : Operation {
    KIELITESTI_SUORITUS_VIEWED,
    ;

    override fun name(): String? = this.name
}

enum class KituAuditLogMessageField(
    val key: String,
) : Operation {
    OPPIJA_OPPIJANUMERO("oppijaHenkiloOid"),
    ;

    override fun name(): String? = this.name
}

class AuditLoggerImpl(
    val logger: SLogger,
) : Logger {
    override fun log(msg: String?) {
        logger.info(msg)
    }
}
