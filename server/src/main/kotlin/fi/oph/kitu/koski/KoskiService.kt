package fi.oph.kitu.koski

import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

@Service
class KoskiService(
    @Qualifier("koskiRestClient")
    private val koskiRestClient: RestClient,
    private val koskiRequestMapper: KoskiRequestMapper,
) {
    fun sendYkiSuoritusToKoski(ykiSuoritusEntity: YkiSuoritusEntity): KoskiResponse {
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

        return koskiResponse.body!!
    }
}
