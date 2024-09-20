package fi.oph.kitu.oppija

import fi.oph.kitu.generated.api.OppijaControllerApi
import fi.oph.kitu.oppijanumerorekisteri.OppijanumerorekisteriService
import fi.oph.kitu.oppijanumerorekisteri.YleistunnisteHaeRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class OppijaController(
    private val oppijanumerorekisteriService: OppijanumerorekisteriService,
    private val oppijaService: OppijaService,
) : OppijaControllerApi {
    override fun getOppijat(): ResponseEntity<List<Oppija>> =
        ResponseEntity(oppijaService.getAll().toList(), HttpStatus.OK)

    override fun addOppija(
        @RequestBody name: String,
    ): ResponseEntity<Oppija> {
        val inserted = oppijaService.insert(name)
        return ResponseEntity(
            inserted,
            if (inserted != null) HttpStatus.CREATED else HttpStatus.BAD_REQUEST,
        )
    }

    @GetMapping("/api/oppija/oppijanumero")
    fun getOppijanumero(): String {
        try {
            val requestData =
                YleistunnisteHaeRequest(
                    "Magdalena Testi",
                    "010866-9260",
                    "Magdalena",
                    "Sallinen-Testi",
                )
            val response = oppijanumerorekisteriService.httpPostOnCasEndpoint(requestData)
            return response.body.toString()
        } catch (e: Exception) {
            println("an error occurred")
            println(e)
            return e.localizedMessage
        }
    }
}
