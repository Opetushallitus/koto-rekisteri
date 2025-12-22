package fi.oph.kitu.dev

import com.nimbusds.jose.jwk.source.ImmutableSecret
import fi.oph.kitu.Oid
import fi.oph.kitu.auth.Authority
import fi.oph.kitu.auth.CasUserDetails
import fi.oph.kitu.dev.MockLoginController.Companion.E2E_TEST_SECRET_KEY
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import java.net.URI
import java.util.Date
import javax.crypto.spec.SecretKeySpec
import kotlin.system.exitProcess

@RestController
@RequestMapping("/dev")
@Profile("local", "e2e")
class MockLoginController(
    private val environment: Environment,
    private val applicationContext: WebApplicationContext,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val securityContextRepository = HttpSessionSecurityContextRepository()

    @Value("\${kitu.appUrl}")
    private lateinit var rootUrl: String

    @PostConstruct
    fun init() {
        if (environment.activeProfiles.any { it == "qa" || it.lowercase().contains("prod") }) {
            logger.error("Fatal error: MockLoginController loaded in a prod-like environment")
            exitProcess(SpringApplication.exit(applicationContext))
        }
    }

    @GetMapping("/mocklogin")
    fun mocklogin(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Unit> = mockLoginForUser(MockUser.DEFAULT, request, response)

    @GetMapping("/mocklogin/{user}")
    fun mocklogin2(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @PathVariable user: MockUser,
    ): ResponseEntity<Unit> = mockLoginForUser(user, request, response)

    private fun mockLoginForUser(
        user: MockUser,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Unit> {
        val userDetails = user.login.toCasUserDetails()
        val authentication =
            UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.authorities,
            )
        SecurityContextHolder.getContext().authentication = authentication
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response)

        return if (user.login.authorities.contains(Authority.VIRKAILIJA)) {
            ResponseEntity.status(HttpStatus.FOUND).location(URI.create(rootUrl)).build()
        } else {
            ResponseEntity.ok().build()
        }
    }

    @PostMapping(
        "/oauth/token",
        consumes = ["application/x-www-form-urlencoded"],
        produces = ["application/json"],
    )
    fun mockOAuth2Token(
        @RequestParam("grant_type") grantType: String,
        @RequestParam("client_id") mockUser: MockUser,
        @RequestParam("client_secret") clientSecret: String,
    ) = mockUser.login.toOAuthTokenResponse()

    companion object {
        // Salainen avain on 256 A-kirjainta
        val E2E_TEST_SECRET_KEY = SecretKeySpec(ByteArray(256) { 65 }, "HmacSHA256")
    }
}

data class MockLogin(
    val name: String,
    val authorities: List<Authority>,
) {
    fun toCasUserDetails() =
        CasUserDetails(
            name = name,
            // Can be any valid OID. Currently oppijanumero for Ranja Testi Öhman-Testi
            oid = Oid.parse("1.2.246.562.24.20281155246").getOrThrow(),
            strongAuth = false,
            kayttajaTyyppi = "VIRKAILIJA",
            authorities = authorities.map { SimpleGrantedAuthority(it.role()) },
        )

    fun toOAuthTokenResponse(): OAuthTokenResponse {
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        val claims =
            JwtClaimsSet
                .builder()
                .subject(name)
                .audience(listOf("test-audience"))
                .expiresAt(Date(System.currentTimeMillis() + 60_000).toInstant())
                .claim("scope", authorities.joinToString(" "))
                .build()
        val jwt =
            NimbusJwtEncoder(ImmutableSecret(E2E_TEST_SECRET_KEY.encoded)).encode(
                JwtEncoderParameters.from(header, claims),
            )
        return OAuthTokenResponse(jwt.tokenValue)
    }
}

enum class MockUser(
    val login: MockLogin,
) {
    DEFAULT(
        MockLogin(
            name = "kitu_mocklogin",
            authorities =
                listOf(
                    Authority.VIRKAILIJA,
                    Authority.YKI_TALLENNUS,
                    Authority.VKT_TALLENNUS,
                ),
        ),
    ),
    ROOT(
        MockLogin(
            name = "kitu_mocklogin",
            authorities =
                listOf(
                    Authority.VIRKAILIJA,
                    Authority.YKI_TALLENNUS,
                    Authority.VKT_TALLENNUS,
                ),
        ),
    ),
    KIOS(
        MockLogin(
            name = "kitu_mocklogin_kios",
            authorities =
                listOf(
                    Authority.VKT_TALLENNUS,
                ),
        ),
    ),
    SOLKI(
        MockLogin(
            name = "kitu_mocklogin_solki",
            authorities =
                listOf(
                    Authority.YKI_TALLENNUS,
                ),
        ),
    ),
    NO_ROLES(
        MockLogin(
            name = "kitu_mocklogin_no_roles",
            authorities =
                listOf(),
        ),
    ),
}

data class OAuthTokenResponse(
    val access_token: String,
    val token_type: String = "bearer",
    val expires_in: Long = 3600,
)
