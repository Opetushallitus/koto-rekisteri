package fi.oph.kitu.oppija

import fi.oph.kitu.generated.api.OppijaControllerApi
import fi.oph.kitu.oppijanumerorekisteri.YleistunnisteHaeRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestTemplate

@RestController
class OppijaController(
    private val restTemplate: RestTemplate,
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
        val virkailijaUrl = "https://virkailija.testiopintopolku.fi/yleistunniste/hae"
        val request =
            YleistunnisteHaeRequest(
                "Magdalena Testi",
                "010866-9260",
                "Magdalena",
                "Sallinen-Testi",
            )

        val response =
            restTemplate
                .postForEntity(virkailijaUrl, request, String::class.java)

        return response.body ?: "empty body"
    }
}

data class YleistunnisteHaeRequest(
    val etunimet: String,
    val hetu: String,
    val kutsumanimi: String,
    val sukunimi: String,
)
