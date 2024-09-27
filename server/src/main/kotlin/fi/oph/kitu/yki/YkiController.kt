package fi.oph.kitu.yki

import fi.oph.kitu.generated.api.YkiControllerApi
import fi.oph.kitu.generated.model.YkiSuoritus
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@RestController
class YkiController(
    @Qualifier("mockRestClient")
    private val restClient: RestClient,
) : YkiControllerApi {
    override fun getSuoritus(): ResponseEntity<YkiSuoritus> {
        val resp =
            restClient
                .get()
                .uri("/yki")
                .retrieve()
                .body<YkiSuoritus>()

        return ResponseEntity(resp, HttpStatus.OK)
    }
}
