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
import org.springframework.security.cas.web.authentication.ServiceAuthenticationDetailsSource
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
    ): CasAuthenticationFilter =
        CasAuthenticationFilter().apply {
            setAuthenticationManager(authenticationConfiguration.authenticationManager)
            setFilterProcessesUrl("/j_spring_cas_security_check")
            setServiceProperties(serviceProperties)
            setAuthenticationDetailsSource(ServiceAuthenticationDetailsSource(serviceProperties))
        }

    @Bean
    fun casAuthenticationProvider(
        serviceProperties: ServiceProperties?,
        ticketValidator: TicketValidator?,
    ): AuthenticationProvider =
        CasAuthenticationProvider().apply {
            setAuthenticationUserDetailsService(CasUserDetailsService())
            setServiceProperties(serviceProperties)
            setTicketValidator(ticketValidator)
            setStatelessTicketCache(CasStatelessTicketCache())
            setKey("kitu")
        }

    @Bean
    fun casTicketValidator(): TicketValidator = Cas30ServiceTicketValidator("https://$opintopolkuHostname/cas")

    @Bean
    fun authenticationEntryPoint(serviceProperties: ServiceProperties): AuthenticationEntryPoint =
        CasAuthenticationEntryPoint().apply {
            loginUrl = "https://$opintopolkuHostname/cas/login"
            setServiceProperties(serviceProperties)
        }
}
