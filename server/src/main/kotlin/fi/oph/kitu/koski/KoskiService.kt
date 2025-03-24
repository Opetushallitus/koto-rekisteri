package fi.oph.kitu.koski

import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.toEntity

@Service
class KoskiService(
    @Qualifier("koskiRestClient")
    private val koskiRestClient: RestClient,
    private val koskiRequestMapper: KoskiRequestMapper,
    private val ykiSuoritusRepository: YkiSuoritusRepository,
) {
    fun sendYkiSuoritusToKoski(ykiSuoritusEntity: YkiSuoritusEntity): TypedResult<YkiSuoritusEntity, KoskiException> {
        val koskiRequest = koskiRequestMapper.ykiSuoritusToKoskiRequest(ykiSuoritusEntity)
        val koskiResponse =
            try {
                koskiRestClient
                    .put()
                    .uri("oppija")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(koskiRequest)
                    .retrieve()
                    .toEntity<KoskiResponse>()
            } catch (e: RestClientException) {
                return TypedResult.Failure(KoskiException(ykiSuoritusEntity.id, e.message))
            }

        val koskiOpiskeluoikeus =
            koskiResponse.body
                ?.opiskeluoikeudet
                ?.first()
                ?.oid

        if (koskiOpiskeluoikeus == null) {
            return TypedResult.Failure(
                KoskiException(ykiSuoritusEntity.id, "KOSKI opiskeluoikeus OID missing from response"),
            )
        }

        val suoritus = ykiSuoritusEntity.copy(koskiOpiskeluoikeus = Oid.parse(koskiOpiskeluoikeus).getOrThrow())
        ykiSuoritusRepository.save(suoritus)
        return TypedResult.Success(suoritus)
    }

    fun sendYkiSuorituksetToKoski() {
        val suoritukset = ykiSuoritusRepository.findSuorituksetWithNoKoskiopiskeluoikeus()
        val results = suoritukset.map { sendYkiSuoritusToKoski(it) }
        val failed = results.filterIsInstance<TypedResult.Failure<YkiSuoritusEntity, KoskiException>>()
        if (failed.isNotEmpty()) throw Error.SendToKOSKIFailed(failed.map { it.error.suoritusId })
    }

    sealed class Error(
        message: String,
    ) : Throwable(message) {
        class SendToKOSKIFailed(
            ids: List<Int?>,
        ) : Error("Failed to send following suoritukset to KOSKI: ${ids.joinToString(",")}")
    }
}
