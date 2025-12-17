package fi.oph.kitu.auth

import jakarta.servlet.http.HttpServletRequest
import org.apereo.cas.client.session.SingleSignOutFilter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.security.cas.web.CasAuthenticationFilter
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import kotlin.collections.contains

@Configuration
@EnableWebSecurity
class WebSecurityConfig {
    @Bean
    fun singleSignOutFilter(): SingleSignOutFilter =
        SingleSignOutFilter().apply {
            setIgnoreInitConfiguration(true)
        }

    @Bean
    @Order(1)
    @ConditionalOnProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri")
    fun oauth2SecurityFilterChain(
        http: HttpSecurity,
        environment: Environment,
    ): SecurityFilterChain {
        http {
            authorizeHttpRequests {
                authorize(anyRequest, authenticated)
            }
            csrf {
                if (environment.activeProfiles.contains("e2e")) {
                    disable()
                }
            }
            securityMatcher({ request ->
                isOauth2Request(request)
            })
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            oauth2ResourceServer {
                jwt {
                    jwtAuthenticationConverter = JwtAuthenticationTokenConverter()
                }
            }
        }
        return http.build()
    }

    @Bean
    @Order(2)
    fun casSecurityFilterChain(
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
                if (environment.activeProfiles.contains("e2e")) {
                    disable()
                }
            }
            logout {
                logoutSuccessUrl = casConfig.getCasLogoutUrl()
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

    private fun isOauth2Request(request: HttpServletRequest): Boolean =
        request.getHeader("Authorization")?.startsWith("Bearer ") ?: false

    /**
     * Konfiguraatio kun palvelua ajetaan HTTPS proxyn läpi. Käytännössä tämä
     * muuttaa [jakarta.servlet.ServletRequest.getScheme] palauttamaan
     * `https` jolloin palvelun kautta luodut urlit muodostuvat oikein.
     *
     *
     * Aktivointi: `kayttooikeus.uses-ssl-proxy` arvoon `true`.
     *
     * @return EmbeddedServletContainerCustomizer jonka Spring automaattisesti
     * tunnistaa ja lisää servlet containerin konfigurointiin
     *
     * Katso myös: [Oppijanumerorekisterin toteutus](https://github.com/Opetushallitus/oppijanumerorekisteri/blob/e2de50dfc039280ff3f657456451d67e6af40bd3/henkilo-ui/src/main/java/fi/vm/sade/henkiloui/configurations/ServletContainerConfiguration.java)
     */
    @Bean
    @ConditionalOnBooleanProperty("kitu.uses-ssl-proxy")
    fun sslProxyCustomizer(): WebServerFactoryCustomizer<TomcatServletWebServerFactory> =
        WebServerFactoryCustomizer { container ->
            container.addConnectorCustomizers(
                TomcatConnectorCustomizer { connector ->
                    connector.scheme = "https"
                    connector.secure = true
                },
            )
        }
}
