package fi.oph.kitu.security

import fi.oph.kitu.oid.Oid
import fi.oph.kitu.security.cas.CasUserDetails
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

object CurrentUser {
    fun hasAuthority(authority: Authority): Boolean {
        val granted = authentication()?.authorities?.map { it.authority }.orEmpty()
        return authority.authStrings().any { it in granted }
    }

    fun oid(): Oid? = (authentication()?.principal as? CasUserDetails)?.oid

    private fun authentication(): Authentication? =
        SecurityContextHolder
            .getContext()
            .authentication
            ?.takeIf { it.isAuthenticated }
}
