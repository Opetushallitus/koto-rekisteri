package fi.oph.kitu.logging

import fi.oph.kitu.Oid
import fi.oph.kitu.auth.CasUserDetails
import fi.vm.sade.auditlog.User
import org.springframework.core.io.ClassPathResource
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.net.InetAddress
import java.util.Properties

data class AuditContext(
    val userOid: Oid,
    val userAgent: String,
    val ip: InetAddress,
    val session: String,
    val opetushallitusOrganisaatioOid: Oid,
) {
    companion object {
        /**
         * Gets the value for `oph.oid` from `application.properties` without using Spring at all.
         */
        private fun getOpetushallitusOrganizationOid(): Oid {
            val props = Properties()

            val resource = ClassPathResource("application.properties")
            resource.inputStream.use { stream ->
                props.load(stream)
            }

            val oid = props.getProperty("oph.oid")
            val parsed = Oid.parse(oid).getOrThrow()
            return parsed
        }

        fun get(): AuditContext {
            val userDetails: CasUserDetails =
                SecurityContextHolder.getContext().authentication?.principal as CasUserDetails?
                    ?: throw IllegalStateException("User details not available via SecurityContextHolder")
            val servletRequestAttributes =
                RequestContextHolder.getRequestAttributes() as ServletRequestAttributes?
                    ?: throw IllegalStateException("HTTP request not available via RequestContextHolder")
            val request = servletRequestAttributes.request

            val userOid = userDetails.oid

            // Used to set organization oid for the logged in user.
            val opetushallitusOrganisaatioOid = getOpetushallitusOrganizationOid()

            val userAgent = request.getHeader("user-agent")
            val ip = InetAddress.getByName(request.remoteAddr)
            val session = request.session.id

            return AuditContext(userOid, userAgent, ip, session, opetushallitusOrganisaatioOid)
        }
    }

    fun user(): User = User(userOid.unwrap(), ip, session, userAgent)
}
