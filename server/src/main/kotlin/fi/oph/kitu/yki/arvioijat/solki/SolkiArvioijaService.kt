package fi.oph.kitu.yki.arvioijat.solki

import fi.oph.kitu.util.TimeService
import fi.oph.kitu.yki.arvioijat.YkiArvioijaEntity
import fi.oph.kitu.yki.arvioijat.YkiArvioijaRepository
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.slf4j.LoggerFactory

enum class Lahetystulos {
    LAHETETTY,
    VIRHE,

    /** Lahetys on kytketty pois: rivi jaa jonoon ja lahtee kun kytkin avataan. */
    EI_KAYTOSSA,
}

interface SolkiArvioijaService {
    /** Yksi synkroninen yritys tallennuksen jalkeen, jotta virkailija nakee tuloksen heti. */
    fun lahetaArvioija(arvioija: YkiArvioijaEntity): Lahetystulos

    /**
     * @param maxYritykset null = kaikki lahettamattomat, myos pitkaan epaonnistuneet.
     * @return onnistuneiden lahetysten maara.
     */
    fun lahetaLahettamattomat(maxYritykset: Int? = null): Int
}

// open, koska @WithSpan vaatii CGLIB-proxyn: ilman @Service-annotaatiota Kotlinin allopen ei
// enaa avaa luokkaa.
open class SolkiArvioijaServiceImpl(
    private val repository: YkiArvioijaRepository,
    private val client: SolkiArvioijaClient,
    private val timeService: TimeService,
) : SolkiArvioijaService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @WithSpan
    override fun lahetaArvioija(arvioija: YkiArvioijaEntity): Lahetystulos = laheta(arvioija)

    @WithSpan
    override fun lahetaLahettamattomat(maxYritykset: Int?): Int {
        val lahetettavat = repository.findLahetettavat(maxYritykset)
        val onnistuneet = lahetettavat.count { laheta(it) == Lahetystulos.LAHETETTY }

        Span
            .current()
            .setAttribute("arvioijat.yritetty", lahetettavat.size.toLong())
            .setAttribute("arvioijat.lahetetty", onnistuneet.toLong())

        return onnistuneet
    }

    /**
     * Yksikin odottamaton poikkeus ei saa keskeyttaa eraa: muuten yksi rikkinainen rivi estaisi
     * koko jonon lahetyksen eivatka muut rivit saisi edes virhemerkintaa.
     */
    private fun laheta(arvioija: YkiArvioijaEntity): Lahetystulos {
        val id = arvioija.id?.toInt() ?: return Lahetystulos.VIRHE

        return runCatching {
            client
                .put(SolkiArvioijaRequest.of(arvioija, timeService.today()))
                .fold(
                    ifLeft = { virhe ->
                        repository.merkitseLahetysvirhe(id, virhe.debugString())
                        Lahetystulos.VIRHE
                    },
                    ifRight = {
                        // Versioehto: jos rivia on muokattu lahetyksen aikana, se jaa jonoon.
                        repository.merkitseLahetetyksi(id, arvioija.muokattu)
                        Lahetystulos.LAHETETTY
                    },
                )
        }.getOrElse { e ->
            logger.warn("Arvioijan {} lahetys epaonnistui odottamattomasti", arvioija.arvioijaOid, e)
            repository.merkitseLahetysvirhe(id, "Unexpected failure: ${e.javaClass.simpleName}")
            Lahetystulos.VIRHE
        }
    }
}

/**
 * Lahetys pois kaytosta. Rivit jaavat lahetysjonoon, joten kytkimen avaaminen lahettaa ne
 * takautuvasti.
 */
open class SolkiArvioijaServiceMock : SolkiArvioijaService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @WithSpan
    override fun lahetaArvioija(arvioija: YkiArvioijaEntity): Lahetystulos {
        logger.debug("lahetaArvioija called but Solki sending is disabled, skipping.")
        return Lahetystulos.EI_KAYTOSSA
    }

    @WithSpan
    override fun lahetaLahettamattomat(maxYritykset: Int?): Int {
        logger.debug("lahetaLahettamattomat called but Solki sending is disabled, skipping.")
        return 0
    }
}
