package fi.oph.kitu.apidocs

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.GlobalOpenApiCustomizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.web.util.UriComponentsBuilder

const val OAUTH_SECURITY_SCHEME = "oauth_client_credentials"

/** Konfiguroidaan OpenAPI/Swagger-autentikaatio ohjelmallisesti, koska autentikaatio riippuu ajonaikaisesta auktorisaatiopalvelimen osoitteesta, joten emme voi laittaa sitä annotaatioon.  */
@OpenAPIDefinition(
    info =
        Info(
            title = "Kielitutkintorekisteri",
            version = "v1",
        ),
)
@Configuration
@ConditionalOnProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri")
class OpenApiSecurityConfig : GlobalOpenApiCustomizer {
    @Value($$"${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    lateinit var oauthIssuerUri: String

    private fun oauthUriBuilder() = UriComponentsBuilder.fromUriString(oauthIssuerUri)

    override fun customise(openApi: OpenAPI) {
        openApi.components.addSecuritySchemes(
            OAUTH_SECURITY_SCHEME,
            SecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .flows(
                    OAuthFlows()
                        .clientCredentials(
                            // En löytänyt tapaa kysyä Spring Securityltä suoraan token-urlia, joten joudumme muodostamaan sen käsin tässä.
                            OAuthFlow()
                                .tokenUrl(
                                    // Javan URI-luokan uri.resolve(path) ylikirjoittaa koko polun, joten käytetään Springin UriComponentsBuilder-helpperiä polkusegmenttien lisäämiseksi loppuun.
                                    oauthUriBuilder()
                                        .pathSegment("oauth2", "token")
                                        .build()
                                        .toUriString(),
                                ).scopes(
                                    Scopes().addString(
                                        "APP_KIELITUTKINTOREKISTERI_YKI_TALLENNUS",
                                        "Tallenna YKI-tutkintoja ja -arvioijia",
                                    ),
                                ),
                        ),
                ),
        )

        val oauthPaths = listOf("/yki/api/suoritus", "/yki/api/arvioija")

        for (path in oauthPaths) {
            openApi.paths
                .get(path)
                ?.post
                ?.security(
                    listOf(
                        SecurityRequirement()
                            .addList(OAUTH_SECURITY_SCHEME, listOf("APP_KIELITUTKINTOREKISTERI_YKI_TALLENNUS")),
                    ),
                )
        }
    }
}
