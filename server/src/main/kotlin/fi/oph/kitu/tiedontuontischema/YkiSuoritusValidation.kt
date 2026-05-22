package fi.oph.kitu.tiedontuontischema

import arrow.core.NonEmptyList
import arrow.core.raise.ExperimentalRaiseAccumulateApi
import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.accumulate
import arrow.core.raise.ensure
import fi.oph.kitu.i18n.LocalizationService
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.organisaatiot.OrganisaatioService
import fi.oph.kitu.organisaatiot.OrganisaatiopalveluException
import fi.oph.kitu.util.intersects
import fi.oph.kitu.util.validation.Validation
import fi.oph.kitu.yki.Arviointitila
import fi.oph.kitu.yki.Tutkintokieli
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class YkiSuoritusValidation(
    val organisaatiot: OrganisaatioService,
    val localizationService: LocalizationService,
    @param:Value("\${kitu.validaatiot.yki.hetunSiirronRajapaiva}")
    val hetunSiirronRajapaiva: LocalDate,
    @param:Value("\${kitu.validaatiot.yki.todistuskielenSiirronRajapaiva}")
    val todistuskielenSiirronRajapaiva: LocalDate,
) : Validation<YkiHenkilosuoritus> {
    @OptIn(ExperimentalRaiseAccumulateApi::class)
    override fun Raise<NonEmptyList<Validation.ValidationError>>.validateBeforeEnrichment(
        value: YkiHenkilosuoritus,
    ): YkiHenkilosuoritus {
        accumulate {
            accumulating { validateHetu(value) }
            accumulating { validateArvointitila(value) }
            accumulating { validateTarkistusarviointi(value) }
            accumulating { validateKielikoodi(value) }
            accumulating { validateTodistuskieli(value) }
            accumulating { validateCountryCode(value) }
        }
        return value
    }

    override fun enrich(value: YkiHenkilosuoritus): YkiHenkilosuoritus {
        val arvosanaKeskeytetty =
            Koodisto.YkiArvosana.Keskeytetty.koodiarvo
                .toInt()

        val osatIlmoittautuneista = value.suoritus.osat.filter { it.arvosana != 12 }
        val arviointitila =
            if (osatIlmoittautuneista.any { it.arvosana == arvosanaKeskeytetty }) {
                Arviointitila.KESKEYTETTY
            } else {
                value.suoritus.arviointitila
            }

        return value.copy(
            suoritus =
                value.suoritus.copy(
                    osat = osatIlmoittautuneista,
                    arviointitila = arviointitila,
                ),
        )
    }

    @OptIn(ExperimentalRaiseAccumulateApi::class)
    override fun Raise<NonEmptyList<Validation.ValidationError>>.validateAfterEnrichment(
        value: YkiHenkilosuoritus,
    ): YkiHenkilosuoritus {
        accumulate {
            accumulating { validateOsakokeitaOnAtLeastOne(value) }
            accumulating { validateArvosanat(value) }
        }
        return value
    }

    private fun Raise<Validation.ValidationError>.validateTodistuskieli(s: YkiHenkilosuoritus) {
        ensure(s.suoritus.tutkintopaiva.isBefore(todistuskielenSiirronRajapaiva) || s.suoritus.todistuskieli != null) {
            Validation.ValidationError(
                listOf("suoritus", "todistuskieli"),
                "Todistuskieli on pakollinen ${todistuskielenSiirronRajapaiva.finnishDate()} alkaen",
            )
        }
    }

    private fun Raise<Validation.ValidationError>.validateHetu(s: YkiHenkilosuoritus) {
        ensure(s.suoritus.tutkintopaiva.isBefore(hetunSiirronRajapaiva) || s.henkilo.hetu == null) {
            Validation.ValidationError(
                listOf("henkilo", "hetu"),
                "Henkilötunnusta ei voi siirtää suoritukselle, jonka tutkintopäivä on " +
                    "${hetunSiirronRajapaiva.finnishDate()} tai myöhemmin",
            )
        }
    }

    @OptIn(ExperimentalRaiseAccumulateApi::class)
    private fun RaiseAccumulate<Validation.ValidationError>.validateArvointitila(s: YkiHenkilosuoritus) {
        if (s.suoritus.arviointitila.arvioitu()) {
            accumulating {
                ensure(s.suoritus.arviointipaiva != null) {
                    Validation.ValidationError(
                        listOf("suoritus", "arviointipaiva"),
                        "Arviointitilan '${s.suoritus.arviointitila}' mukaan suoritus on arvioitu, " +
                            "mutta arviointipäivä puuttuu",
                    )
                }
            }
            s.suoritus.osat.forEachIndexed { i, osakoe ->
                accumulating {
                    ensure(osakoe.arvosana != null) {
                        Validation.ValidationError(
                            listOf("suoritus", "osat", i.toString(), "arvosana"),
                            "Arviointitilan '${s.suoritus.arviointitila}' mukaan suoritus on arvioitu, " +
                                "mutta arviointi puuttuu osakokeelta '${osakoe.tyyppi.name}'",
                        )
                    }
                }
            }
        } else {
            ensure(s.suoritus.arviointipaiva == null) {
                Validation.ValidationError(
                    listOf("suoritus", "arviointipaiva"),
                    "Arviointitilan '${s.suoritus.arviointitila}' mukaan suoritusta ei ole vielä arvioitu, " +
                        "mutta arviointipäivä on määritelty",
                )
            }
        }
    }

    @OptIn(ExperimentalRaiseAccumulateApi::class)
    private fun RaiseAccumulate<Validation.ValidationError>.validateTarkistusarviointi(s: YkiHenkilosuoritus) {
        accumulating {
            val tarkastettavatOsakokeet =
                s.suoritus.tarkistusarviointi
                    ?.tarkistusarvioidutOsakokeet
                    .orEmpty()
            val muuttuneetOsakokeet =
                s.suoritus.tarkistusarviointi
                    ?.arvosanaMuuttui
                    .orEmpty()
            ensure(muuttuneetOsakokeet.minus(tarkastettavatOsakokeet).isEmpty()) {
                Validation.ValidationError(
                    listOf("suoritus", "tarkistusarviointi", "arvosanaMuuttui"),
                    "Muuttuneet arvosanat sisälsivät osakokeita, jotka eivät olleet osa tarkistettavia osakokeita",
                )
            }
        }
        accumulating {
            ensure(
                (s.suoritus.tarkistusarviointi?.saapumispaiva ?: LocalDate.MIN) <=
                    (s.suoritus.tarkistusarviointi?.kasittelypaiva ?: LocalDate.MAX),
            ) {
                Validation.ValidationError(
                    listOf("suoritus", "tarkistusarviointi", "kasittelypaiva"),
                    "Käsittelypäivä on ennen saapumispäivää",
                )
            }
        }
    }

    private fun Raise<Validation.ValidationError>.validateKielikoodi(s: YkiHenkilosuoritus) {
        ensure(!s.suoritus.kieli.isLegacy() || s.suoritus.tutkintopaiva.isBefore(LocalDate.of(2017, 1, 1))) {
            Validation.ValidationError(
                listOf("suoritus", "kieli"),
                "Käytöstä poistuneita kielikoodeja (${Tutkintokieli.Companion.legacyEntries.joinToString(
                    ", ",
                )}) ei voi käyttää",
            )
        }
    }

    private fun Raise<Validation.ValidationError>.validateCountryCode(s: YkiHenkilosuoritus) {
        if (s.henkilo.maa == null) return
        val koodisto =
            localizationService
                .translationBuilder()
                .koodistot("maatjavaltiot1")
                .build()
        val maatJaValtiot = koodisto.koodistot["maatjavaltiot1"] ?: return
        ensure(maatJaValtiot[s.henkilo.maa] != null) {
            Validation.ValidationError(
                listOf("henkilo", "maa"),
                "Virheellinen maakoodi",
            )
        }
    }

    private fun Raise<Validation.ValidationError>.validateOsakokeitaOnAtLeastOne(s: YkiHenkilosuoritus) {
        ensure(s.suoritus.osat.isNotEmpty()) {
            Validation.ValidationError(
                listOf("suoritus", "osat"),
                "Suorituksella täytyy olla vähintään yksi osakoe, johon on ilmottauduttu",
            )
        }
    }

    private fun Raise<Validation.ValidationError>.validateArvosanat(s: YkiHenkilosuoritus) {
        val tutkintotaso = s.suoritus.tutkintotaso
        val validArvosanat = Koodisto.YkiArvosana.validIntegersFor(tutkintotaso)
        val invalidArvosanat = s.suoritus.osat.mapNotNull { if (it.arvosana in validArvosanat) null else it.arvosana }
        ensure(invalidArvosanat.isEmpty()) {
            Validation.ValidationError(
                listOf("suoritus", "osat", "arvosana"),
                "Suoritus sisältää tutkintotasolle $tutkintotaso virheellisiä arvosanoja: " +
                    invalidArvosanat.joinToString(", "),
            )
        }
    }
}
