package fi.oph.kitu.logging
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.Oid
import fi.oph.kitu.TestTimeService
import fi.oph.kitu.auth.CasUserDetails
import fi.oph.kitu.i18n.isoDateTimeUTC
import fi.oph.kitu.mock.generateRandomOppijaOid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant
import kotlin.random.Random
import kotlin.test.assertEquals
import ch.qos.logback.classic.Logger as LogbackLogger

@SpringBootTest
@Import(DBContainerConfiguration::class)
@ExtendWith(OutputCaptureExtension::class)
class AuditLoggerTests(
    @param:Autowired val timeService: TestTimeService,
) {
    private val logbackLogger = LoggerFactory.getLogger(AUDIT_LOGGER_NAME) as LogbackLogger

    private val listAppender = ListAppender<ILoggingEvent>()

    @BeforeEach
    fun setup() {
        listAppender.start()
        logbackLogger.addAppender(listAppender)
    }

    @AfterEach
    fun cleanup() {
        listAppender.stop()
        logbackLogger.detachAppender(listAppender)
    }

    @Test
    fun `log logs JSON string correctly`(
        @Autowired auditLogger: AuditLogger,
    ) {
        timeService.runWithFixedClock(Instant.parse("2025-09-29T12:00:00Z")) {
            RequestContextHolder.setRequestAttributes(
                ServletRequestAttributes(
                    MockHttpServletRequest().apply {
                        addHeader(
                            HttpHeaders.USER_AGENT,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                                "(KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36",
                        )
                    },
                ),
            )

            val oid = Oid.parseTyped("1.2.246.562.24.19563255030").getOrThrow()
            val principal =
                CasUserDetails(
                    name = "test",
                    oid = oid,
                    strongAuth = false,
                    kayttajaTyyppi = "user_type",
                    authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
                )
            SecurityContextHolder
                .getContext()
                .authentication =
                UsernamePasswordAuthenticationToken(
                    principal,
                    "test_credentials",
                    principal.authorities,
                )

            auditLogger.log(
                AuditLogOperation.KielitestiSuoritusViewed,
                generateRandomOppijaOid(Random(123456789)),
            )

            val events = listAppender.list

            assertEquals(1, events.size)

            val expectedJson =
                """
            {
                "version":1,
                "logSeq":0,
                "bootTime":"${auditLogger.bootTime.isoDateTimeUTC()}",
                "type":"log",
                "environment":"test",
                "hostname":"http://localhost:8080/kielitutkinnot",
                "timestamp":"2025-09-29T12:00:00Z"
                ,"serviceName":"kitu",
                "applicationType":"backend",
                "user":{
                    "oid":"1.2.246.562.24.19563255030"
                },
                "target":{
                    "oppijaHenkiloOid":"1.2.246.562.24.19563255030"
                },
                "organizationOid":"1.2.246.562.10.00000000001",
                "operation":"KielitestiSuoritusViewed"
            }
            """.replace("\n", "")
                    .replace(" ", "")

            assertEquals(expectedJson, events[0].formattedMessage)
        }
    }
}
