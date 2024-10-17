package fi.oph.kitu.dev

import fi.oph.kitu.generated.api.YkiControllerApi
import fi.oph.kitu.yki.YkiService
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.SpringApplication
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.WebApplicationContext
import java.time.LocalDate
import kotlin.system.exitProcess

@RestController
@Profile("local", "e2e")
class MockYkiControllerApi(
    private val environment: Environment,
    private val applicationContext: WebApplicationContext,
    private val ykiService: YkiService,
) : YkiControllerApi {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        if (environment.activeProfiles.any { it == "qa" || it.lowercase().contains("prod") }) {
            logger.error("Fatal error: ${this::class.simpleName} loaded in a prod-like environment")
            exitProcess(SpringApplication.exit(applicationContext))
        }
    }

    override fun getArvioijat() =
        ResponseEntity
            .ok()
            .headers(HttpHeaders().apply { set("dry-run", "true") })
            .body(
                """"09876","010101A961P","Arvioija","Arttu","arttu.arvioija@yki.fi","Testiosoite 7357","00100","HELSINKI",0,"rus","PT+KT" """,
            )

    override fun getSuoritukset(arvioitu: LocalDate?): ResponseEntity<String> =
        ResponseEntity
            .ok()
            .headers(HttpHeaders().apply { set("dry-run", "true") })
            .body(
                """"1.2.246.562.24.99999999999","Suorittaja","Sulevi",2022-11-12,"fin","KT","1.2.246.562.10.373218511910","Iisalmen kansalaisopisto",2,2,1,3,2,2""",
            )

    override fun triggerSchedule(
        dryRun: Boolean?,
        lastSeen: LocalDate?,
    ): ResponseEntity<Unit> = ResponseEntity(ykiService.importYkiSuoritukset(lastSeen, dryRun), HttpStatus.OK)
}
