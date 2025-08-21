package fi.oph.kitu.logging

import fi.oph.kitu.Oid
import fi.oph.kitu.auth.CasUserDetails
import fi.vm.sade.auditlog.ApplicationType
import fi.vm.sade.auditlog.Audit
import fi.vm.sade.auditlog.Changes
import fi.vm.sade.auditlog.Logger
import fi.vm.sade.auditlog.Operation
import fi.vm.sade.auditlog.User
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.task.AsyncTaskExecutor
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
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
        operation: KituAuditLogOperation,
        target: Iterable<Pair<KituAuditLogMessageField, String>>,
        changes: Changes = Changes.EMPTY,
    ) = log(AuditContext.get(), operation, target, changes)

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
        operation: KituAuditLogOperation,
        entities: Iterable<E>,
        changes: Changes = Changes.EMPTY,
        properties: (E) -> Array<Pair<KituAuditLogMessageField, Any?>>,
    ) {
        val context = AuditContext.get()

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
    companion object {
        fun get(): AuditContext {
            val userDetails =
                SecurityContextHolder.getContext().authentication?.principal as CasUserDetails?
                    ?: throw IllegalStateException("User details not available via SecurityContextHolder")
            val servletRequestAttributes =
                RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?
                    ?: throw IllegalStateException("HTTP request not available via RequestContextHolder")
            val request = servletRequestAttributes.request

            val userOid = Oid.parse(userDetails.oid).getOrThrow()
            val userAgent = request.getHeader("user-agent")
            val ip = InetAddress.getByName(request.remoteAddr)
            val session = request.session.id

            return AuditContext(userOid, userAgent, ip, session)
        }
    }

    fun user(): User = User(userOid.unwrap(), ip, session, userAgent)
}

sealed class KituAuditLogOperation(
    val name: String,
) : Operation {
    override fun name(): String? = name

    class KielitestiSuoritusViewed : KituAuditLogOperation("KielitestiSuoritusViewed")
}

enum class KituAuditLogMessageField(
    val key: String,
) {
    OPPIJA_OPPIJANUMERO("oppijaHenkiloOid"),
}

class AuditLoggerImpl(
    val logger: SLogger,
) : Logger {
    override fun log(msg: String?) {
        logger.info(msg)
    }
}
