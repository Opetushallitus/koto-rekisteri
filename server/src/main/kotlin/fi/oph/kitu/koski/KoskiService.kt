package fi.oph.kitu.koski

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.tiedonsiirtoschema.VktHenkilosuoritus
import fi.oph.kitu.util.result.getOrThrow
import fi.oph.kitu.util.result.splitIntoValuesAndErrors
import fi.oph.kitu.util.retry.RetryOutboundIntegration
import fi.oph.kitu.vkt.CustomVktSuoritusRepository
import fi.oph.kitu.vkt.VktSuoritusService
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.toEntity
import java.time.LocalDateTime

@Service
class KoskiService(
    @param:Qualifier("koskiRestClient")
    private val koskiRestClient: RestClient,
    private val koskiYkiRequestMapper: KoskiYkiRequestMapper,
    private val koskiVktRequestMapper: KoskiVktRequestMapper,
    private val ykiSuoritusRepository: YkiSuoritusRepository,
    private val customVktSuoritusRepository: CustomVktSuoritusRepository,
    private val vktSuoritusService: VktSuoritusService,
    private val koskiErrors: KoskiErrorService,
) {
    @WithSpan
    @RetryOutboundIntegration
    fun sendYkiSuoritusToKoski(ykiSuoritusEntity: YkiSuoritusEntity): Either<KoskiException, YkiSuoritusEntity> {
        val koskiRequest = koskiYkiRequestMapper.ykiSuoritusToKoskiRequest(ykiSuoritusEntity).getOrNull()

        if (koskiRequest == null) {
            val suoritus = ykiSuoritusEntity.copy(koskiSiirtoKasitelty = true)
            ykiSuoritusRepository.save(suoritus, true)
            return suoritus.right()
        }

        val koskiResponse =
            when (val result = putToKoski(YkiMappingId(ykiSuoritusEntity.solkiId), koskiRequest)) {
                is Either.Right -> result.value
                is Either.Left -> return result.value.left()
            }

        val koskiOpiskeluoikeus =
            koskiResponse.body
                ?.opiskeluoikeudet
                ?.first()
                ?.oid
                ?: return KoskiException(
                    YkiMappingId(ykiSuoritusEntity.solkiId),
                    "KOSKI opiskeluoikeus OID missing from response",
                ).left()

        val suoritus =
            ykiSuoritusEntity.copy(
                koskiOpiskeluoikeus = Oid.parse(koskiOpiskeluoikeus).getOrThrow(),
                koskiSiirtoKasitelty = true,
            )
        ykiSuoritusRepository.save(suoritus, true)
        return suoritus.right()
    }

    @WithSpan
    @RetryOutboundIntegration
    fun sendYkiMitatointiToKoski(ykiSuoritusEntity: YkiSuoritusEntity): Either<KoskiException, Unit> {
        if (ykiSuoritusEntity.koskiOpiskeluoikeus != null) {
            koskiYkiRequestMapper
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

        return Unit.right()
    }

    @WithSpan
    @RetryOutboundIntegration
    fun sendVktSuoritusToKoski(suoritus: VktHenkilosuoritus): Either<KoskiException, Unit> {
        val id = CustomVktSuoritusRepository.Tutkintoryhma.from(suoritus)
        val koskiRequest = koskiVktRequestMapper.vktSuoritusToKoskiRequest(suoritus)
        if (koskiRequest.isLeft()) {
            // Suoritus ei ole vielä valmis lähetettäväksi, mutta se ei ole tiedonsiirtovirhe.
            return Unit.right()
        }

        val koskiResponse =
            when (val result = putToKoski(VktMappingId(id), koskiRequest.getOrThrow())) {
                is Either.Right -> result.value
                is Either.Left -> return result.value.left()
            }

        val koskiOpiskeluoikeusOid =
            koskiResponse.body
                ?.opiskeluoikeudet
                ?.firstOrNull()
                ?.oid

        vktSuoritusService.markKoskiTransferProcessed(id, koskiOpiskeluoikeusOid)
        return Unit.right()
    }

    @WithSpan
    fun sendYkiSuorituksetToKoski(): KoskiTransferReport {
        val suoritukset = ykiSuoritusRepository.findKoskeenLahettamattomatSuoritukset()
        val results = suoritukset.map { sendYkiSuoritusToKoski(it) }
        return reportErrors(results.map { it.map { suoritus -> YkiMappingId(suoritus.solkiId) } })
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

    private fun putToKoski(
        id: KoskiErrorMappingId,
        koskiRequest: KoskiRequest,
    ): Either<KoskiException, ResponseEntity<KoskiResponse>> =
        try {
            koskiRestClient
                .put()
                .uri("oppija")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(koskiRequest)
                .retrieve()
                .toEntity<KoskiResponse>()
                .right()
        } catch (e: RestClientException) {
            KoskiException.from(id, e).left()
        }

    private inline fun <reified T : KoskiErrorMappingId> reportErrors(
        results: List<Either<KoskiException, T>?>,
    ): KoskiTransferReport {
        val (success, failed) = results.filterNotNull().splitIntoValuesAndErrors()
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
