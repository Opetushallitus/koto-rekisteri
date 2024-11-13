package fi.oph.kitu.oppijanumero

import fi.oph.kitu.generated.api.OppijanumeroControllerApi
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class OppijanumeroController(
    private val oppijanumeroService: OppijanumeroService,
) : OppijanumeroControllerApi {
    private val event = LoggerFactory.getLogger(javaClass).atInfo()

    override fun getOppijanumero(): ResponseEntity<String> {
        try {
            val response =
                oppijanumeroService.yleistunnisteHae(
                    YleistunnisteHaeRequest(
                        "Magdalena Testi",
                        "010866-9260",
                        "Magdalena",
                        "Sallinen-Testi",
                    ),
                )
            return ResponseEntity(
                response.body(),
                if (response.statusCode() == 200) HttpStatus.OK else HttpStatus.INTERNAL_SERVER_ERROR,
            )
        } catch (e: Exception) {
            event.setCause(e)

            return ResponseEntity(
                "An unexpected error has occurred:${e.localizedMessage}",
                HttpStatus.INTERNAL_SERVER_ERROR,
            )
        } finally {
            event.log()
        }
    }
}
