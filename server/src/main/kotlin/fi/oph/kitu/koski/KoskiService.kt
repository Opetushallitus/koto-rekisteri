package fi.oph.kitu.koski

import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.mapValues
import fi.oph.kitu.observability.use
import fi.oph.kitu.partitionBySuccess
import fi.oph.kitu.vkt.CustomVktSuoritusRepository
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.VktSuoritusService
import fi.oph.kitu.vkt.tiedonsiirtoschema.Henkilosuoritus
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

@Service
class KoskiService(
    @Qualifier("koskiRestClient")
    private val koskiRestClient: RestClient,
    private val koskiRequestMapper: KoskiRequestMapper,
    private val ykiSuoritusRepository: YkiSuoritusRepository,
    private val tracer: Tracer,
    private val customVktSuoritusRepository: CustomVktSuoritusRepository,
    private val vktSuoritusService: VktSuoritusService,
    private val koskiErrors: KoskiErrorService,
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
                            return TypedResult.Failure(KoskiException(YkiMappingId(ykiSuoritusEntity.id), e.message))
                        }

                    val koskiOpiskeluoikeus =
                        koskiResponse.body
                            ?.opiskeluoikeudet
                            ?.first()
                            ?.oid

                    if (koskiOpiskeluoikeus == null) {
                        return TypedResult.Failure(
                            KoskiException(
                                YkiMappingId(ykiSuoritusEntity.id),
                                "KOSKI opiskeluoikeus OID missing from response",
                            ),
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

    fun sendVktSuoritusToKoski(suoritus: Henkilosuoritus<VktSuoritus>): TypedResult<Unit, KoskiException> =
        tracer
            .spanBuilder("KoskiService.sendVktSuoritusToKoski")
            .startSpan()
            .use { span ->
                val id = CustomVktSuoritusRepository.Tutkintoryhma.from(suoritus)
                val koskiRequest = koskiRequestMapper.vktSuoritusToKoskiRequest(suoritus)
                if (koskiRequest == null) {
                    vktSuoritusService.markKoskiTransferProcessed(id)
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
                        return TypedResult.Failure(KoskiException(VktMappingId(id), e.message))
                    }

                val koskiOpiskeluoikeusOid =
                    koskiResponse.body
                        ?.opiskeluoikeudet
                        ?.firstOrNull()
                        ?.oid

                vktSuoritusService.markKoskiTransferProcessed(id, koskiOpiskeluoikeusOid)
                return TypedResult.Success(Unit)
            }

    @WithSpan
    fun sendYkiSuorituksetToKoski() {
        val suoritukset = ykiSuoritusRepository.findSuorituksetWithNoKoskiopiskeluoikeus()
        val results = suoritukset.map { sendYkiSuoritusToKoski(it) }
        reportErrors(results.mapValues { YkiMappingId(it.suoritusId) })
    }

    @WithSpan
    fun sendVktSuorituksetToKoski() {
        val siirrettavat = customVktSuoritusRepository.findOpiskeluoikeudetForKoskiTransfer()
        val results =
            siirrettavat.map { id ->
                vktSuoritusService.getOppijanSuoritukset(id)?.let { suoritus ->
                    sendVktSuoritusToKoski(suoritus).map {
                        VktMappingId(CustomVktSuoritusRepository.Tutkintoryhma.from(suoritus))
                    }
                }
            }
        reportErrors(results)
    }

    private inline fun <reified T : KoskiErrorMappingId> reportErrors(results: List<TypedResult<T, KoskiException>?>) {
        val (success, failed) = results.filterNotNull().partitionBySuccess()
        success.forEach { id -> koskiErrors.reset(id) }
        failed.forEach { error -> koskiErrors.save(error.suoritusId, error.message ?: error.toString()) }
        if (failed.isNotEmpty()) throw Error.SendToKOSKIFailed(failed.map { it.suoritusId.mappedId() })
    }

    sealed class Error(
        message: String,
    ) : Throwable(message) {
        class SendToKOSKIFailed(
            ids: List<String?>,
        ) : Error("Failed to send following suoritukset to KOSKI: ${ids.joinToString(",")}")
    }
}
