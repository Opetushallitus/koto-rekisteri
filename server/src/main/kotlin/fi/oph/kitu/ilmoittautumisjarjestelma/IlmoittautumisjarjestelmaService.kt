package fi.oph.kitu.ilmoittautumisjarjestelma

import fi.oph.kitu.TypedResult
import fi.oph.kitu.yki.suoritukset.YkiSuoritusEntity
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

interface IlmoittautumisjarjestelmaService {
    fun sendAllUpdatedArvioinninTilat()

    fun sendArvioinninTila(suoritus: YkiSuoritusEntity)
}

@Service
@ConditionalOnBean(IlmoittautumisjarjestelmaClient::class)
class IlmoittautumisjarjestelmaServiceImpl(
    val suoritusRepository: YkiSuoritusRepository,
    val client: IlmoittautumisjarjestelmaClient,
) : IlmoittautumisjarjestelmaService {
    @WithSpan
    override fun sendAllUpdatedArvioinninTilat() {
        val suoritukset = suoritusRepository.findSuorituksetWithUnsentArvioinninTila()
        if (suoritukset.isNotEmpty()) {
            val response = sendArvioinninTilat(YkiArvioinninTilaRequest.of(suoritukset))
            saveResponse(suoritukset, response)
        }
    }

    @WithSpan
    override fun sendArvioinninTila(suoritus: YkiSuoritusEntity) {
        val response = sendArvioinninTilat(YkiArvioinninTilaRequest.of(suoritus))
        saveResponse(listOf(suoritus), response)
    }

    @WithSpan
    private fun sendArvioinninTilat(
        request: YkiArvioinninTilaRequest,
    ): TypedResult<out IlmoittautumisjarjestelmaResponse, out IlmoittautumisjarjestelmaException> =
        client.post(
            "yki/v2/api/oauth2/registration/evaluation",
            request,
            IlmoittautumisjarjestelmaResponse::class.java,
        )

    private fun saveResponse(
        suoritukset: List<YkiSuoritusEntity>,
        response: TypedResult<out IlmoittautumisjarjestelmaResponse, out IlmoittautumisjarjestelmaException>,
    ) = response.fold({ response ->
        val virheIds =
            response.virheet
                ?.let { virheet ->
                    val tunnisteIds = suoritukset.associate { YkiSuorituksenTunniste.of(it) to it.suoritusId }
                    virheet.associate { tunnisteIds[it.suoritus] to it.virhe }
                }.orEmpty()

        val (failedSuoritukset, okSuoritukset) = suoritukset.partition { virheIds.containsKey(it.suoritusId) }

        if (okSuoritukset.isNotEmpty()) {
            suoritusRepository.setArvioinninTilaSent(okSuoritukset.map { it.suoritusId })
        }
        virheIds.forEach { suoritusId, virhe ->
            suoritusId?.let {
                suoritusRepository.setArvioinninTilanLahetysvirhe(suoritusId, virhe)
            }
        }
    }, { exception ->
        suoritukset.forEach { suoritus ->
            suoritusRepository.setArvioinninTilanLahetysvirhe(
                suoritus.suoritusId,
                exception.debugString(),
            )
        }
    })
}

@Service
@ConditionalOnMissingBean(IlmoittautumisjarjestelmaClient::class)
class IlmoittautumisjarjestelmaServiceMock : IlmoittautumisjarjestelmaService {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    @WithSpan
    override fun sendAllUpdatedArvioinninTilat() {
        logger.debug("sendAllUpdatedArvioinninTilat called but no client configured, skipping.")
    }

    @WithSpan
    override fun sendArvioinninTila(suoritus: YkiSuoritusEntity) {
        logger.debug("sendArvioinninTila called but no client configured, skipping.")
    }
}
