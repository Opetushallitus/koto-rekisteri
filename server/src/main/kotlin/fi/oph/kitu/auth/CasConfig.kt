package fi.oph.kitu.auth

import org.apereo.cas.client.validation.Cas30ServiceTicketValidator
import org.apereo.cas.client.validation.TicketValidator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.cas.ServiceProperties
import org.springframework.security.cas.authentication.CasAuthenticationProvider
import org.springframework.security.cas.web.CasAuthenticationEntryPoint
import org.springframework.security.cas.web.CasAuthenticationFilter
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.web.AuthenticationEntryPoint

@Configuration
class CasConfig {
    @Value("\${kitu.appUrl}")
    lateinit var appUrl: String

    @Value("\${kitu.opintopolkuHostname}")
    lateinit var opintopolkuHostname: String

    @Bean
    fun serviceProperties() =
        ServiceProperties().apply {
            service = "$appUrl/j_spring_cas_security_check"
            isSendRenew = false
            isAuthenticateAllArtifacts = true
        }

    @Bean
    fun getCasLogoutUrl() = "https://$opintopolkuHostname/cas/logout?service=$appUrl"

    @Bean
    fun casAuthenticationFilter(
        authenticationConfiguration: AuthenticationConfiguration,
        serviceProperties: ServiceProperties,
    ): CasAuthenticationFilter {
        val casAuthenticationFilter = CasAuthenticationFilter()
        casAuthenticationFilter.setAuthenticationManager(authenticationConfiguration.authenticationManager)
        casAuthenticationFilter.setFilterProcessesUrl("/j_spring_cas_security_check")
        casAuthenticationFilter.setServiceProperties(serviceProperties)
        return casAuthenticationFilter
    }

    @Bean
    fun casAuthenticationProvider(
        serviceProperties: ServiceProperties?,
        ticketValidator: TicketValidator?,
    ): AuthenticationProvider {
        val casAuthenticationProvider = CasAuthenticationProvider()
        casAuthenticationProvider.setAuthenticationUserDetailsService(CasUserDetailsService())
        casAuthenticationProvider.setServiceProperties(serviceProperties)
        casAuthenticationProvider.setTicketValidator(ticketValidator)
        casAuthenticationProvider.setKey("kitu")
        return casAuthenticationProvider
    }

    @Bean
    fun casTicketValidator(): TicketValidator = Cas30ServiceTicketValidator("https://$opintopolkuHostname/cas")

    @Bean
    fun authenticationEntryPoint(serviceProperties: ServiceProperties): AuthenticationEntryPoint {
        val entryPoint = CasAuthenticationEntryPoint()
        entryPoint.loginUrl = "https://$opintopolkuHostname/cas/login"
        entryPoint.serviceProperties = serviceProperties
        return entryPoint
    }
}
