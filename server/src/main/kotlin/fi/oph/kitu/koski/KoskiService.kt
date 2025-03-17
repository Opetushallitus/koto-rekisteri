package fi.oph.kitu.koski

import fi.oph.kitu.Oid
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
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
    private val ykiSuoritusRepository: YkiSuoritusRepository,
) {
    fun sendYkiSuoritusToKoski(ykiSuoritusEntity: YkiSuoritusEntity): YkiSuoritusEntity {
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

        val koskiOpiskeluoikeus =
            koskiResponse.body!!
                .opiskeluoikeudet
                .first()
                .oid

        val suoritus = ykiSuoritusEntity.copy(koskiOpiskeluoikeus = Oid.parse(koskiOpiskeluoikeus).getOrThrow())
        ykiSuoritusRepository.save(suoritus)
        return suoritus
    }
}
