package fi.oph.kitu.auth

import fi.oph.kitu.dev.MockLoginController.Companion.E2E_TEST_SECRET_KEY
import fi.oph.kitu.dev.MockUser
import jakarta.servlet.http.HttpServletRequest
import org.apereo.cas.client.session.SingleSignOutFilter
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpMethod.PUT
import org.springframework.security.cas.web.CasAuthenticationFilter
import org.springframework.security.config.annotation.web.AuthorizeHttpRequestsDsl
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import kotlin.collections.contains

fun AuthorizeHttpRequestsDsl.configureCommonAuthorizations(roleType: RoleType) {
    authorize(PUT, "/api/vkt/kios", hasAuthority(Authority.VKT_TALLENNUS.by(roleType)))
    authorize(POST, "/yki/api/suoritus", hasAuthority(Authority.YKI_TALLENNUS.by(roleType)))
    authorize(POST, "/yki/api/arvioija", hasAuthority(Authority.YKI_TALLENNUS.by(roleType)))

    authorize("/actuator/health", permitAll)
    authorize("/api-docs", permitAll)
    authorize("/swagger-ui/**", permitAll)
    authorize("/v3/api-docs/**", permitAll)
    authorize("/schema-examples/**", permitAll)
}

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
                configureCommonAuthorizations(RoleType.OAUTH2)
                authorize(anyRequest, denyAll)
            }
            csrf {
                disable()
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
                configureCommonAuthorizations(RoleType.CAS)

                if ((
                        environment.activeProfiles.contains("local") ||
                            environment.activeProfiles.contains("test") ||
                            environment.activeProfiles.contains("e2e")
                    ) &&
                    !environment.activeProfiles.any { it == "qa" || it.lowercase().contains("prod") }
                ) {
                    authorize("/dev/**", permitAll)
                }

                authorize(anyRequest, hasAuthority(Authority.VIRKAILIJA.role()))
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

@Configuration
@ConditionalOnMissingBean(JwtDecoder::class)
@Profile("test", "e2e")
class TestJwtConfig {
    @Bean
    fun jwtDecoder(): NimbusJwtDecoder {
        val decoder = NimbusJwtDecoder.withSecretKey(E2E_TEST_SECRET_KEY).build()

        // Throws and halts application startup if the token is invalid.
        decoder.decode(
            MockUser.ROOT.login
                .toOAuthTokenResponse()
                .access_token,
        )

        return decoder
    }
}
