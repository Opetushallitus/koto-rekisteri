package fi.oph.kitu.dev

import fi.oph.kitu.auth.CasUserDetails
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import java.net.URI
import kotlin.system.exitProcess

@RestController
@RequestMapping("dev")
@Profile("local", "e2e")
class MockLoginController(
    private val environment: Environment,
    private val applicationContext: WebApplicationContext,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)
    private val securityContextRepository = HttpSessionSecurityContextRepository()

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
    ): ResponseEntity<Unit> {
        val userDetails =
            CasUserDetails(
                name = "kitu_mocklogin",
                oid = "",
                strongAuth = false,
                kayttajaTyyppi = "VIRKAILIJA",
                authorities =
                    listOf(
                        SimpleGrantedAuthority("ROLE_APP_KIELITUTKINTOREKISTERI"),
                        SimpleGrantedAuthority("ROLE_APP_KIELITUTKINTOREKISTERI_READ"),
                        SimpleGrantedAuthority("ROLE_APP_KIELITUTKINTOREKISTERI_READ_1.2.246.562.10.00000000001"),
                        SimpleGrantedAuthority("ROLE_APP_KIELITUTKINTOREKISTERI_VKT"),
                    ),
            )
        val authentication = UsernamePasswordAuthenticationToken(userDetails, null, userDetails.authorities)
        SecurityContextHolder.getContext().authentication = authentication
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response)
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/")).build()
    }
}
