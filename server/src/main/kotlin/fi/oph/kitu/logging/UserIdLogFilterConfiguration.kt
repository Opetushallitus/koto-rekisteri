package fi.oph.kitu.logging

import fi.oph.kitu.auth.CasUserDetails
import fi.oph.kitu.auth.CasUserDetailsService
import jakarta.servlet.Filter
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.MDC
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken
import org.springframework.security.cas.authentication.CasAuthenticationToken
import org.springframework.security.core.GrantedAuthority

@Configuration
class UserIdLogFilterConfiguration {
    @Bean
    fun userIdLogFilter(userDetailsService: CasUserDetailsService) =
        Filter { request, response, filterChain ->
            val token = (request as? HttpServletRequest)?.userPrincipal
            MDC.put("user.auth_type", token?.javaClass?.kotlin?.simpleName ?: "not authenticated")
            val user =
                when (token) {
                    is CasAuthenticationToken -> token.userDetails as CasUserDetails
                    is CasAssertionAuthenticationToken ->
                        userDetailsService.loadUserDetails(
                            token,
                        ) as CasUserDetails

                    else -> null
                }

            if (user != null) {
                MDC.put("user.id", user.oid)
                MDC.put("user.roles", authoritiesToString(user.authorities))

                MDC.put("user.strong_auth", user.strongAuth.toString())
                MDC.put("user.type", user.kayttajaTyyppi)
            }

            filterChain.doFilter(request, response)
            MDC.clear()
        }

    private fun authoritiesToString(authorities: List<GrantedAuthority>): String =
        authorities
            .joinToString(",", transform = { "\"${it.authority}\"" })
            .let { "[$it]" }
}
