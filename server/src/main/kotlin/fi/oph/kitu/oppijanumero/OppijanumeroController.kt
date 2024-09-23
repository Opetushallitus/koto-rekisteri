package fi.oph.kitu.oppijanumero

import fi.oph.kitu.generated.api.OppijanumeroControllerApi
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class OppijanumeroController(
    private val oppijanumeroService: OppijanumeroService,
) : OppijanumeroControllerApi {
    override fun getOppijanumero(): ResponseEntity<String> {
        try {
            val response = oppijanumeroService.yleistunnisteHae()
            return ResponseEntity(
                response,
                HttpStatus.OK,
            )
        } catch (e: Exception) {
            println("ERROR: ${e.message}")

            return ResponseEntity(
                "An unexpected error has occurred:${e.localizedMessage}",
                HttpStatus.INTERNAL_SERVER_ERROR,
            )
        }
    }
}
