package fi.oph.kitu.koski

import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.mapValues
import fi.oph.kitu.observability.use
import fi.oph.kitu.partitionBySuccess
import fi.oph.kitu.retry.RetryOutboundIntegration
import fi.oph.kitu.vkt.CustomVktSuoritusRepository
import fi.oph.kitu.vkt.VktHenkilosuoritus
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
import java.time.LocalDateTime

@Service
class KoskiService(
    @param:Qualifier("koskiRestClient")
    private val koskiRestClient: RestClient,
    private val koskiRequestMapper: KoskiRequestMapper,
    private val ykiSuoritusRepository: YkiSuoritusRepository,
    private val tracer: Tracer,
    private val customVktSuoritusRepository: CustomVktSuoritusRepository,
    private val vktSuoritusService: VktSuoritusService,
    private val koskiErrors: KoskiErrorService,
) {
    @WithSpan
    @RetryOutboundIntegration
    fun sendYkiSuoritusToKoski(ykiSuoritusEntity: YkiSuoritusEntity): TypedResult<YkiSuoritusEntity, KoskiException> =
        tracer
            .spanBuilder("KoskiService.sendYkiSuoritusToKoski")
            .startSpan()
            .use { span ->
                val koskiRequest = koskiRequestMapper.ykiSuoritusToKoskiRequest(ykiSuoritusEntity).getOrNull()

                if (koskiRequest == null) {
                    val suoritus = ykiSuoritusEntity.copy(koskiSiirtoKasitelty = true)
                    ykiSuoritusRepository.save(suoritus, true)
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
                            return TypedResult.Failure(
                                KoskiException.from(YkiMappingId(ykiSuoritusEntity.solkiId), e),
                            )
                        }

                    val koskiOpiskeluoikeus =
                        koskiResponse.body
                            ?.opiskeluoikeudet
                            ?.first()
                            ?.oid

                    if (koskiOpiskeluoikeus == null) {
                        return TypedResult.Failure(
                            KoskiException(
                                YkiMappingId(ykiSuoritusEntity.solkiId),
                                "KOSKI opiskeluoikeus OID missing from response",
                            ),
                        )
                    }

                    val suoritus =
                        ykiSuoritusEntity.copy(
                            koskiOpiskeluoikeus = Oid.parse(koskiOpiskeluoikeus).getOrThrow(),
                            koskiSiirtoKasitelty = true,
                        )
                    ykiSuoritusRepository.save(suoritus, true)
                    return TypedResult.Success(suoritus)
                }
            }

    @WithSpan
    @RetryOutboundIntegration
    fun sendYkiMitatointiToKoski(ykiSuoritusEntity: YkiSuoritusEntity): TypedResult<Unit, KoskiException> =
        tracer
            .spanBuilder("KoskiService.sendYkiMitatointiToKoski")
            .startSpan()
            .use { span ->
                if (ykiSuoritusEntity.koskiOpiskeluoikeus != null) {
                    koskiRequestMapper
                        .ykiSuoritusToKoskiRequest(ykiSuoritusEntity)
                        .getOrNull()
                        ?.mitatoi()
                        ?.let { koskiRequest ->
                            koskiRestClient
                                .put()
                                .uri("oppija")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .body(koskiRequest)
                                .retrieve()
                                .toEntity<KoskiResponse>()
                        }

                    val suoritus =
                        ykiSuoritusEntity.copy(
                            koskiOpiskeluoikeus = null,
                            koskiSiirtoKasitelty = false,
                        )
                    ykiSuoritusRepository.save(suoritus, true)
                }

                TypedResult.Success(Unit)
            }

    @WithSpan
    @RetryOutboundIntegration
    fun sendVktSuoritusToKoski(suoritus: VktHenkilosuoritus): TypedResult<Unit, KoskiException> =
        tracer
            .spanBuilder("KoskiService.sendVktSuoritusToKoski")
            .startSpan()
            .use { span ->
                val id = CustomVktSuoritusRepository.Tutkintoryhma.from(suoritus)
                val koskiRequest = koskiRequestMapper.vktSuoritusToKoskiRequest(suoritus)
                if (koskiRequest.isFailure) {
                    // Suoritus ei ole vielä valmis lähetettäväksi, mutta se ei ole tiedonsiirtovirhe.
                    return TypedResult.Success(Unit)
                }

                val koskiResponse =
                    try {
                        koskiRestClient
                            .put()
                            .uri("oppija")
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .body(koskiRequest.getOrThrow())
                            .retrieve()
                            .toEntity<KoskiResponse>()
                    } catch (e: RestClientException) {
                        return TypedResult.Failure(KoskiException.from(VktMappingId(id), e))
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
    fun sendYkiSuorituksetToKoski(): KoskiTransferReport {
        val suoritukset = ykiSuoritusRepository.findKoskeenLahettamattomatSuoritukset()
        val results = suoritukset.map { sendYkiSuoritusToKoski(it) }
        return reportErrors(results.mapValues { YkiMappingId(it.solkiId) })
    }

    @WithSpan
    fun sendVktSuorituksetToKoski(): KoskiTransferReport {
        val siirrettavat = customVktSuoritusRepository.findOpiskeluoikeudetForKoskiTransfer()
        val results =
            siirrettavat.map { id ->
                vktSuoritusService.getOppijanSuoritukset(id, false)?.let { suoritus ->
                    sendVktSuoritusToKoski(suoritus).map {
                        VktMappingId(CustomVktSuoritusRepository.Tutkintoryhma.from(suoritus))
                    }
                }
            }
        return reportErrors(results)
    }

    private inline fun <reified T : KoskiErrorMappingId> reportErrors(
        results: List<TypedResult<T, KoskiException>?>,
    ): KoskiTransferReport {
        val (success, failed) = results.filterNotNull().partitionBySuccess()
        success.forEach { id -> koskiErrors.reset(id) }
        failed.forEach { error -> koskiErrors.save(error.suoritusId, error.message ?: error.toString()) }
        failed.find { it is KoskiTechnicalException }?.let { throw it }
        return KoskiTransferReport(
            success.size,
            results.size,
        )
    }
}

data class KoskiTransferReport(
    val successfulTransfers: Int,
    val totalCount: Int,
    val timestamp: LocalDateTime = LocalDateTime.now(),
) {
    override fun toString(): String =
        (
            listOfNotNull(
                "Viimeisin onnistunut eräajo: $timestamp",
                "Siirretty $successfulTransfers / $totalCount",
            )
        ).joinToString("; ")
}
