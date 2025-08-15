package fi.oph.kitu.koski

import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.observability.use
import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.vkt.CustomVktSuoritusRepository
import fi.oph.kitu.vkt.VktSuoritusRepository
import fi.oph.kitu.vkt.VktSuoritusService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.toEntity
import kotlin.jvm.optionals.getOrNull

@Service
class KoskiService(
    @Qualifier("koskiRestClient")
    private val koskiRestClient: RestClient,
    private val koskiRequestMapper: KoskiRequestMapper,
    private val ykiSuoritusRepository: YkiSuoritusRepository,
    private val tracer: Tracer,
    private val vktSuoritusRepository: VktSuoritusRepository,
    private val customVktSuoritusRepository: CustomVktSuoritusRepository,
    private val vktSuoritusService: VktSuoritusService,
    private val onrService: OppijanumeroService,
) {
    fun sendYkiSuoritusToKoski(ykiSuoritusEntity: YkiSuoritusEntity): TypedResult<YkiSuoritusEntity, KoskiException> =
        tracer
            .spanBuilder("KoskiService.sendYkiSuoritusToKoski")
            .startSpan()
            .use { span ->
                val koskiRequest = koskiRequestMapper.ykiSuoritusToKoskiRequest(ykiSuoritusEntity)

                if (koskiRequest == null) {
                    val suoritus = ykiSuoritusEntity.copy(koskiSiirtoKasitelty = true)
                    ykiSuoritusRepository.save(suoritus)
                    return TypedResult.Success(suoritus)
                } else {
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

                    val suoritus =
                        ykiSuoritusEntity.copy(
                            koskiOpiskeluoikeus = Oid.parse(koskiOpiskeluoikeus).getOrThrow(),
                            koskiSiirtoKasitelty = true,
                        )
                    ykiSuoritusRepository.save(suoritus)
                    return TypedResult.Success(suoritus)
                }
            }

    fun sendVktSuoritusToKoski(vktSuoritusId: Int): TypedResult<Unit, KoskiException> =
        tracer
            .spanBuilder("KoskiService.sendVktSuoritusToKoski")
            .startSpan()
            .use { span ->
                val suoritus = vktSuoritusService.getSuoritus(vktSuoritusId).getOrNull()
                if (suoritus == null) {
                    return TypedResult.Failure(KoskiException(vktSuoritusId, "VKT suoritus disappeared"))
                }

                val koskiRequest = koskiRequestMapper.vktSuoritusToKoskiRequest(suoritus, onrService)
                if (koskiRequest == null) {
                    vktSuoritusService.setSuoritusTransferredToKoski(vktSuoritusId)
                    return TypedResult.Success(Unit)
                }

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
                        return TypedResult.Failure(KoskiException(vktSuoritusId, e.message))
                    }

                val koskiOpiskeluoikeusOid =
                    koskiResponse.body
                        ?.opiskeluoikeudet
                        ?.firstOrNull()
                        ?.oid

                vktSuoritusService.setSuoritusTransferredToKoski(vktSuoritusId, koskiOpiskeluoikeusOid)
                return TypedResult.Success(Unit)
            }

    @WithSpan
    fun sendYkiSuorituksetToKoski() {
        val suoritukset = ykiSuoritusRepository.findSuorituksetWithNoKoskiopiskeluoikeus()
        val results = suoritukset.map { sendYkiSuoritusToKoski(it) }
        val failed = results.filterIsInstance<TypedResult.Failure<YkiSuoritusEntity, KoskiException>>()
        if (failed.isNotEmpty()) throw Error.SendToKOSKIFailed(failed.map { it.error.suoritusId })
    }

    @WithSpan
    fun sendVktSuorituksetToKoski() {
        val ids = customVktSuoritusRepository.findSuoritusIdsWithNoKoskiopiskeluoikeus()
        val results =
            ids.map { suoritusId ->
                vktSuoritusRepository.findById(suoritusId).getOrNull()?.let { suoritus ->
                    sendVktSuoritusToKoski(suoritusId)
                }
            }
        val failed = results.filterIsInstance<TypedResult.Failure<Unit, KoskiException>>()
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
