package fi.oph.kitu.vkt

import com.nimbusds.jose.jwk.source.ImmutableSecret
import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.auth.Authority
import fi.oph.kitu.dev.MockLoginController
import fi.oph.kitu.schema.SchemaTests
import fi.oph.kitu.util.defaultObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.client.ClientHttpResponse
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.web.client.DefaultResponseErrorHandler
import org.springframework.web.client.RestTemplate
import org.testcontainers.postgresql.PostgreSQLContainer
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DBContainerConfiguration::class)
@ActiveProfiles("test")
@TestPropertySource(
    properties = [
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=test-issuer",
        "server.servlet.context-path=/kielitutkinnot",
    ],
)
class VktAuthorizationTest {
    @Autowired
    private lateinit var vktRepository: VktSuoritusRepository

    @Autowired private var postgres: PostgreSQLContainer? = null

    @LocalServerPort
    private var port: Int = 0

    private val restTemplate =
        RestTemplate().apply {
            errorHandler =
                object : DefaultResponseErrorHandler() {
                    override fun hasError(response: ClientHttpResponse): Boolean = false
                }
        }

    @BeforeEach
    fun setup() {
        vktRepository.deleteAll()
    }

    @Test
    fun `PUT api vkt kios is allowed for user with VKT_TALLENNUS authority`() {
        val response =
            putSuoritus(tokenWithAuthorities(Authority.VKT_TALLENNUS))
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Autowired
    private lateinit var cookieSerializer: org.springframework.session.web.http.CookieSerializer

    @Autowired
    private lateinit var sessionRepositoryFilter: org.springframework.session.web.http.SessionRepositoryFilter<*>

    @Test
    fun `Spring Session is active and cookie name is SESSION`() {
        // Regression guard: Spring Boot 4 requires spring-boot-starter-session-jdbc to auto-wire
        // Spring Session. Without it the app silently falls back to Tomcat's JSESSIONID, which
        // breaks clients (e.g. KIOS) that read the SESSION cookie.
        val request =
            org.springframework.mock.web
                .MockHttpServletRequest()
        val response =
            org.springframework.mock.web
                .MockHttpServletResponse()
        cookieSerializer.writeCookieValue(
            org.springframework.session.web.http.CookieSerializer
                .CookieValue(request, response, "test-session-id"),
        )
        assertEquals("SESSION", response.cookies.single().name)
        // Ensure the filter is also wired, not just the serializer.
        requireNotNull(sessionRepositoryFilter)
    }

    @Test
    fun `PUT api vkt kios is forbidden for user without VKT_TALLENNUS authority`() {
        val response =
            putSuoritus(tokenWithAuthorities(Authority.VIRKAILIJA))
        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
    }

    private fun putSuoritus(bearer: String): ResponseEntity<String> {
        val suoritusJson = defaultObjectMapper.writeValueAsString(SchemaTests.vktHenkilosuoritus)
        val headers =
            HttpHeaders().apply {
                contentType = MediaType.APPLICATION_JSON
                accept = listOf(MediaType.APPLICATION_JSON)
                setBearerAuth(bearer)
            }
        val url = "http://localhost:$port/kielitutkinnot/api/vkt/kios"
        return restTemplate.exchange(
            url,
            HttpMethod.PUT,
            HttpEntity(suoritusJson, headers),
            String::class.java,
        )
    }

    private fun tokenWithAuthorities(vararg authorities: Authority): String {
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        val claims =
            JwtClaimsSet
                .builder()
                .subject("test-user")
                .audience(listOf("test-audience"))
                .expiresAt(Date(System.currentTimeMillis() + 60_000).toInstant())
                .claim("scope", authorities.joinToString(" ") { it.role() })
                .build()
        return NimbusJwtEncoder(ImmutableSecret(MockLoginController.E2E_TEST_SECRET_KEY.encoded))
            .encode(JwtEncoderParameters.from(header, claims))
            .tokenValue
    }
}
