package fi.oph.kitu.koski

import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

@Service
class KoskiService(
    @Qualifier("koskiRestClient")
    private val koskiRestClient: RestClient,
    private val koskiRequestMapper: KoskiRequestMapper,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun sendYkiSuoritusToKoski(ykiSuoritusEntity: YkiSuoritusEntity): ResponseEntity<KoskiResponse> {
        val koskiRequest = koskiRequestMapper.ykiSuoritusToKoskiRequest(ykiSuoritusEntity)
        val koskiResponse =
            koskiRestClient
                .put()
                .uri("oppija")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(koskiRequest)
                .retrieve()
                .toEntity<KoskiResponse>()

        return koskiResponse
    }
}
