package fi.oph.kitu.yki.suoritukset

import arrow.core.NonEmptyList
import arrow.core.raise.ExperimentalRaiseAccumulateApi
import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import fi.oph.kitu.i18n.LocalizationService
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.organisaatiot.OrganisaatioService
import fi.oph.kitu.organisaatiot.OrganisaatiopalveluException
import fi.oph.kitu.tiedontuontischema.YkiHenkilosuoritus
import fi.oph.kitu.tiedontuontischema.YkiSuoritus
import fi.oph.kitu.util.intersects
import fi.oph.kitu.util.validation.Validation
import fi.oph.kitu.util.validation.Validation.ValidationError
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
    override fun Raise<NonEmptyList<ValidationError>>.validateBeforeEnrichment(
        value: YkiHenkilosuoritus,
    ): YkiHenkilosuoritus =
        zipOrAccumulate(
            { validateOrganisaatiot(value) },
            { validateHetu(value) },
            { validateArvointitila(value) },
            { validateTarkistusarviointi(value) },
            { validateKielikoodi(value) },
            { validateTodistuskieli(value) },
            { validateCountryCode(value) },
            { validateArvosanat(value) },
        ) { _, _, _, _, _, _, _, modified -> modified }

    override fun enrich(value: YkiHenkilosuoritus): YkiHenkilosuoritus {
        val arvosanaKeskeytetty =
            Koodisto.YkiArvosana.Keskeytetty.koodiarvo
                .toInt()

        val suoritus: YkiSuoritus = value.suoritus

        return if (suoritus.osat.any { it.arvosana == arvosanaKeskeytetty }) {
            value.copy(suoritus = suoritus.copy(arviointitila = Arviointitila.KESKEYTETTY))
        } else {
            value
        }
    }

    private fun Raise<ValidationError>.validateTodistuskieli(s: YkiHenkilosuoritus) {
        ensure(s.suoritus.tutkintopaiva.isBefore(todistuskielenSiirronRajapaiva) || s.suoritus.todistuskieli != null) {
            ValidationError(
                listOf("suoritus", "todistuskieli"),
                "Todistuskieli on pakollinen ${todistuskielenSiirronRajapaiva.finnishDate()} alkaen",
            )
        }
    }

    private fun Raise<ValidationError>.validateHetu(s: YkiHenkilosuoritus) {
        ensure(s.suoritus.tutkintopaiva.isBefore(hetunSiirronRajapaiva) || s.henkilo.hetu == null) {
            ValidationError(
                listOf("henkilo", "hetu"),
                "Henkilötunnusta ei voi siirtää suoritukselle, jonka tutkintopäivä on " +
                    "${hetunSiirronRajapaiva.finnishDate()} tai myöhemmin",
            )
        }
    }

    private fun Raise<ValidationError>.validateOrganisaatiot(s: YkiHenkilosuoritus) {
        val suoritus = s.suoritus
        val sallitutOrganisaatiotyypit =
            listOf(
                Koodisto.Organisaatiotyyppi.Oppilaitos,
                Koodisto.Organisaatiotyyppi.Toimipiste,
            )

        val oid = suoritus.jarjestaja.oid

        organisaatiot.getOrganisaatio(oid).fold(
            ifRight = { org ->
                val tyypit = org.tyypit.mapNotNull { Koodisto.Organisaatiotyyppi.of(it) }
                ensure(tyypit.intersects(sallitutOrganisaatiotyypit)) {
                    ValidationError(
                        listOf("suoritus", "jarjestaja", "oid"),
                        "Organisaatio $oid on väärän tyyppinen: ${
                            tyypit.joinToString(", ") { it.name }
                        }. Sallitut tyypit: ${
                            sallitutOrganisaatiotyypit.joinToString(", ") { it.name }
                        }.",
                    )
                }
            },
            ifLeft = { exception ->
                raise(
                    ValidationError(
                        listOf("suoritus", "jarjestaja", "oid"),
                        when (exception) {
                            is OrganisaatiopalveluException.NotFoundException -> {
                                "Organisaatiota ${suoritus.jarjestaja.oid} ei löydy organisaatiopalvelusta"
                            }

                            else -> {
                                "Organisaation validointi epäonnistui"
                            }
                        },
                    ),
                )
            },
        )
    }

    @OptIn(ExperimentalRaiseAccumulateApi::class)
    private fun RaiseAccumulate<ValidationError>.validateArvointitila(s: YkiHenkilosuoritus) {
        if (s.suoritus.arviointitila.arvioitu()) {
            accumulating {
                ensure(s.suoritus.arviointipaiva != null) {
                    ValidationError(
                        listOf("suoritus", "arviointipaiva"),
                        "Arviointitilan '${s.suoritus.arviointitila}' mukaan suoritus on arvioitu, " +
                            "mutta arviointipäivä puuttuu",
                    )
                }
            }
            s.suoritus.osat.forEachIndexed { i, osakoe ->
                accumulating {
                    ensure(osakoe.arvosana != null) {
                        ValidationError(
                            listOf("suoritus", "osat", i.toString(), "arvosana"),
                            "Arviointitilan '${s.suoritus.arviointitila}' mukaan suoritus on arvioitu, " +
                                "mutta arviointi puuttuu osakokeelta '${osakoe.tyyppi.name}'",
                        )
                    }
                }
            }
        } else {
            ensure(s.suoritus.arviointipaiva == null) {
                ValidationError(
                    listOf("suoritus", "arviointipaiva"),
                    "Arviointitilan '${s.suoritus.arviointitila}' mukaan suoritusta ei ole vielä arvioitu, " +
                        "mutta arviointipäivä on määritelty",
                )
            }
        }
    }

    @OptIn(ExperimentalRaiseAccumulateApi::class)
    private fun RaiseAccumulate<ValidationError>.validateTarkistusarviointi(s: YkiHenkilosuoritus) {
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
                ValidationError(
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
                ValidationError(
                    listOf("suoritus", "tarkistusarviointi", "kasittelypaiva"),
                    "Käsittelypäivä on ennen saapumispäivää",
                )
            }
        }
    }

    private fun Raise<ValidationError>.validateKielikoodi(s: YkiHenkilosuoritus) {
        ensure(!s.suoritus.kieli.isLegacy() || s.suoritus.tutkintopaiva.isBefore(LocalDate.of(2017, 1, 1))) {
            ValidationError(
                listOf("suoritus", "kieli"),
                "Käytöstä poistuneita kielikoodeja (${Tutkintokieli.legacyEntries.joinToString(", ")}) ei voi käyttää",
            )
        }
    }

    private fun Raise<ValidationError>.validateCountryCode(s: YkiHenkilosuoritus) {
        if (s.henkilo.maa == null) return
        val koodisto =
            localizationService
                .translationBuilder()
                .koodistot("maatjavaltiot1")
                .build()
        val maatJaValtiot = koodisto.koodistot["maatjavaltiot1"] ?: return
        ensure(maatJaValtiot[s.henkilo.maa] != null) {
            ValidationError(
                listOf("henkilo", "maa"),
                "Virheellinen maakoodi",
            )
        }
    }

    private fun Raise<ValidationError>.validateArvosanat(s: YkiHenkilosuoritus): YkiHenkilosuoritus {
        val osakokeet = s.suoritus.osat.mapNotNull { if (it.arvosana == 12) null else it }
        ensure(osakokeet.isNotEmpty()) {
            ValidationError(
                listOf("suoritus", "osat"),
                "Suorituksella täytyy olla vähintään yksi osakoe, johon on ilmottauduttu",
            )
        }
        val validArvosanat = Koodisto.YkiArvosana.validIntegers
        val invalidArvosanat = osakokeet.mapNotNull { if (it.arvosana in validArvosanat) null else it.arvosana }
        ensure(invalidArvosanat.isEmpty()) {
            ValidationError(
                listOf("suoritus", "osat", "arvosana"),
                "Suoritus sisältää virheellisiä arvosanoja: ${invalidArvosanat.joinToString(", ")}",
            )
        }
        return s.copy(suoritus = s.suoritus.copy(osat = osakokeet))
    }
}
