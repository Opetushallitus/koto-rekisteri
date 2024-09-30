package fi.oph.kitu.auth

import org.apereo.cas.client.session.SingleSignOutFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.cas.web.CasAuthenticationFilter
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher

@Configuration
@EnableWebSecurity
class WebSecurityConfig {
    @Bean
    fun singleSignOutFilter(): SingleSignOutFilter =
        SingleSignOutFilter().apply {
            setIgnoreInitConfiguration(true)
        }

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        casAuthenticationFilter: CasAuthenticationFilter,
        singleSignOutFilter: SingleSignOutFilter,
        authenticationEntryPoint: AuthenticationEntryPoint,
        casConfig: CasConfig,
        environment: Environment,
    ): SecurityFilterChain {
        http
            .csrf { csrf -> csrf.ignoringRequestMatchers("/api/*") }
            .logout {
                it.logoutSuccessUrl(casConfig.getCasLogoutUrl())
                it.logoutUrl("/logout")
                it.logoutRequestMatcher(AntPathRequestMatcher("/logout", "GET"))
            }.authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/actuator/health")
                    .permitAll()
            }.addFilter(casAuthenticationFilter)
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
            }.addFilterBefore(singleSignOutFilter, CasAuthenticationFilter::class.java)

        if (
            (environment.activeProfiles.contains("local") || environment.activeProfiles.contains("e2e")) &&
            !environment.activeProfiles.any { it == "qa" || it.lowercase().contains("prod") }
        ) {
            http.authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers("/dev/**")
                    .permitAll()
            }
        }

        http.authorizeHttpRequests { it.anyRequest().authenticated() }

        return http.build()
    }
}
