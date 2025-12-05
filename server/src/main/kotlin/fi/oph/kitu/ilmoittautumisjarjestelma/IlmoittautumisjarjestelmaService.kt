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

    fun sendArvioinninTilat(
        request: YkiArvioinninTilaRequest,
    ): TypedResult<out IlmoittautumisjarjestelmaResponse?, out IlmoittautumisjarjestelmaException>
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
        sendArvioinninTilat(YkiArvioinninTilaRequest.of(suoritukset))
        suoritusRepository.setArvioinninTilaSent(suoritukset.map { it.suoritusId })
    }

    @WithSpan
    override fun sendArvioinninTila(suoritus: YkiSuoritusEntity) {
        sendArvioinninTilat(YkiArvioinninTilaRequest.of(suoritus))
        suoritusRepository.setArvioinninTilaSent(suoritus.suoritusId)
    }

    @WithSpan
    override fun sendArvioinninTilat(
        request: YkiArvioinninTilaRequest,
    ): TypedResult<out IlmoittautumisjarjestelmaResponse?, out IlmoittautumisjarjestelmaException> =
        if (request.isNotEmpty()) {
            client.post("/api/arviointitila", request, IlmoittautumisjarjestelmaResponse::class.java)
        } else {
            TypedResult.Success(null)
        }
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

    @WithSpan
    override fun sendArvioinninTilat(
        request: YkiArvioinninTilaRequest,
    ): TypedResult<IlmoittautumisjarjestelmaResponse?, out IlmoittautumisjarjestelmaException> {
        logger.debug("sendArvioinninTilat called but no client configured, skipping.")
        return TypedResult.Success(null)
    }
}
