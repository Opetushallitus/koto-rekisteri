package fi.oph.kitu.auth

import org.apereo.cas.client.session.SingleSignOutFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.cas.web.CasAuthenticationFilter
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
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
        http {
            csrf {
                ignoringRequestMatchers(
                    "/api/**",
                    "/db-scheduler-api/**",
                )
            }
            logout {
                logoutSuccessUrl = casConfig.getCasLogoutUrl()
                logoutUrl = "/logout"
                logoutRequestMatcher = AntPathRequestMatcher("/logout", "GET")
            }
            authorizeHttpRequests {
                authorize("/actuator/health", permitAll)

                if ((
                        environment.activeProfiles.contains("local") ||
                            environment.activeProfiles.contains("test") ||
                            environment.activeProfiles.contains("e2e")
                    ) &&
                    !environment.activeProfiles.any { it == "qa" || it.lowercase().contains("prod") }
                ) {
                    authorize("/dev/**", permitAll)
                }

                authorize("/api/vkt/**", hasRole("APP_KIELITUTKINTOREKISTERI_VKT_KIELITUTKINTOJEN_KIRJOITUS"))
                authorize("/api-docs", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/schema-examples/**", permitAll)
                authorize(anyRequest, hasRole("APP_KIELITUTKINTOREKISTERI_READ"))
            }
            exceptionHandling {
                this.authenticationEntryPoint = authenticationEntryPoint
            }
            addFilterBefore<CasAuthenticationFilter>(singleSignOutFilter)
        }

        http.addFilter(casAuthenticationFilter)

        return http.build()
    }
}
