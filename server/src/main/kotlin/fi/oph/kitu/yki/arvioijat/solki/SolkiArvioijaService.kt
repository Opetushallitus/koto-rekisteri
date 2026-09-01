package fi.oph.kitu.yki.arvioijat.solki

import fi.oph.kitu.util.TimeService
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.stereotype.Service

interface SolkiArvioijaService {
    /** Yksi synkroninen yritys tallennuksen jalkeen, jotta virkailija nakee tuloksen heti. */
    fun lahetaArvioija(arvioija: YkiArvioijaEntity)

    /** @param maxYritykset null = kaikki lahettamattomat, myos pitkaan epaonnistuneet. */
    fun lahetaLahettamattomat(maxYritykset: Int? = null): Int
}

@Service
@ConditionalOnBean(SolkiArvioijaClient::class)
class SolkiArvioijaServiceImpl(
    private val repository: YkiArvioijaRepository,
    private val client: SolkiArvioijaClient,
    private val timeService: TimeService,
) : SolkiArvioijaService {
    @WithSpan
    override fun lahetaArvioija(arvioija: YkiArvioijaEntity) {
        val id = arvioija.id?.toInt() ?: return

        client
            .put(SolkiArvioijaRequest.of(arvioija, timeService.today()))
            .fold(
                ifLeft = { virhe -> repository.merkitseLahetysvirhe(id, virhe.debugString()) },
                ifRight = { repository.merkitseLahetetyksi(id) },
            )
    }

    @WithSpan
    override fun lahetaLahettamattomat(maxYritykset: Int?): Int {
        val lahetettavat = repository.findLahetettavat(maxYritykset)
        lahetettavat.forEach { lahetaArvioija(it) }

        Span.current().setAttribute("arvioijat.lahetetty", lahetettavat.size.toLong())
        return lahetettavat.size
    }
}

/**
 * Ilman clientia lahetys on kokonaan pois kaytosta. Rivit jaavat lahetysjonoon, joten kytkimen
 * avaaminen lahettaa ne takautuvasti.
 */
@Service
@ConditionalOnMissingBean(SolkiArvioijaClient::class)
class SolkiArvioijaServiceMock : SolkiArvioijaService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @WithSpan
    override fun lahetaArvioija(arvioija: YkiArvioijaEntity) {
        logger.debug("lahetaArvioija called but Solki sending is disabled, skipping.")
    }

    @WithSpan
    override fun lahetaLahettamattomat(maxYritykset: Int?): Int {
        logger.debug("lahetaLahettamattomat called but Solki sending is disabled, skipping.")
        return 0
    }
}
