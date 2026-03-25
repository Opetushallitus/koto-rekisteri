package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.i18n.LocalizationService
import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.intersects
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.organisaatiot.OrganisaatioService
import fi.oph.kitu.organisaatiot.OrganisaatiopalveluException
import fi.oph.kitu.validation.Validation
import fi.oph.kitu.validation.ValidationResult
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
    override fun validationBeforeEnrichment(value: YkiHenkilosuoritus): ValidationResult<YkiHenkilosuoritus> =
        Validation.fold(
            value,
            { validateOrganisaatiot(it) },
            { validateHetu(it) },
            { validateArvointitila(it) },
            { validateTarkistusarviointi(it) },
            { validateKielikoodi(it) },
            { validateTodistuskieli(it) },
            { validateCountryCode(it) },
            { validateArvosanat(it) },
        )

    fun validateTodistuskieli(s: YkiHenkilosuoritus): ValidationResult<YkiHenkilosuoritus> =
        if (s.suoritus.tutkintopaiva.isBefore(todistuskielenSiirronRajapaiva) || s.suoritus.todistuskieli != null) {
            Validation.ok(s)
        } else {
            Validation.fail(
                listOf("suoritus", "todistuskieli"),
                "Todistuskieli on pakollinen ${todistuskielenSiirronRajapaiva.finnishDate()} alkaen",
            )
        }

    fun validateHetu(s: YkiHenkilosuoritus): ValidationResult<YkiHenkilosuoritus> =
        if (s.suoritus.tutkintopaiva.isBefore(hetunSiirronRajapaiva) || s.henkilo.hetu == null) {
            Validation.Companion.ok(s)
        } else {
            Validation.Companion.fail(
                listOf("henkilo", "hetu"),
                "Henkilötunnusta ei voi siirtää suoritukselle, jonka tutkintopäivä on ${hetunSiirronRajapaiva.finnishDate()} tai myöhemmin",
            )
        }

    fun validateOrganisaatiot(s: YkiHenkilosuoritus): ValidationResult<YkiHenkilosuoritus> {
        val suoritus = s.suoritus
        val sallitutOrganisaatiotyypit =
            listOf(
                Koodisto.Organisaatiotyyppi.Oppilaitos,
                Koodisto.Organisaatiotyyppi.Toimipiste,
            )

        val oid = suoritus.jarjestaja.oid

        fun fail(reason: String): ValidationResult<YkiHenkilosuoritus> =
            Validation.Companion.fail(
                listOf("suoritus", "jarjestaja", "oid"),
                reason,
            )

        return organisaatiot.getOrganisaatio(oid).fold(
            onSuccess = { org ->
                val tyypit = org.tyypit.mapNotNull { Koodisto.Organisaatiotyyppi.of(it) }
                if (tyypit.intersects(sallitutOrganisaatiotyypit)) {
                    Validation.Companion.ok(s)
                } else {
                    fail(
                        "Organisaatio $oid on väärän tyyppinen: ${
                            tyypit.joinToString(", ") { it.name }
                        }. Sallitut tyypit: ${
                            sallitutOrganisaatiotyypit.joinToString(", ") { it.name}
                        }.",
                    )
                }
            },
            onFailure = {
                fail(
                    when (it) {
                        is OrganisaatiopalveluException.NotFoundException -> {
                            "Organisaatiota ${suoritus.jarjestaja.oid} ei löydy organisaatiopalvelusta"
                        }

                        else -> {
                            "Organisaation validointi epäonnistui"
                        }
                    },
                )
            },
        )
    }

    fun validateArvointitila(s: YkiHenkilosuoritus): ValidationResult<YkiHenkilosuoritus> =
        if (s.suoritus.arviointitila.arvioitu()) {
            Validation.fold(
                s,
                Validation.assertTrue(
                    { it.suoritus.arviointipaiva != null },
                    listOf("suoritus", "arviointipaiva"),
                    "Arviointitilan '${s.suoritus.arviointitila}' mukaan suoritus on arvioitu, mutta arviointipäivä puuttuu",
                ),
                *(
                    s.suoritus.osat
                        .mapIndexed { i, osakoe ->
                            Validation.assertTrue<YkiHenkilosuoritus>(
                                { osakoe.arvosana != null },
                                listOf("suoritus", "osat", i.toString(), "arvosana"),
                                "Arviointitilan '${s.suoritus.arviointitila}' mukaan suoritus on arvioitu, mutta arviointi puuttuu osakokeelta '${osakoe.tyyppi.name}'",
                            )
                        }.toTypedArray()
                ),
            )
        } else {
            Validation.fold(
                s,
                Validation.assertTrue(
                    { it.suoritus.arviointipaiva == null },
                    listOf("suoritus", "arviointipaiva"),
                    "Arviointitilan '${s.suoritus.arviointitila}' mukaan suoritusta ei ole vielä arvioitu, mutta arviointipäivä on määritelty",
                ),
            )
        }

    fun validateTarkistusarviointi(s: YkiHenkilosuoritus): ValidationResult<YkiHenkilosuoritus> =
        Validation.fold(
            s,
            Validation.assertTrue(
                {
                    val tarkastettavatOsakokeet =
                        it.suoritus.tarkistusarviointi
                            ?.tarkistusarvioidutOsakokeet
                            .orEmpty()
                    val muuttuneetOsakokeet =
                        it.suoritus.tarkistusarviointi
                            ?.arvosanaMuuttui
                            .orEmpty()

                    muuttuneetOsakokeet.minus(tarkastettavatOsakokeet).isEmpty()
                },
                path = listOf("suoritus", "tarkistusarviointi", "arvosanaMuuttui"),
                message =
                    "Muuttuneet arvosanat sisälsivät osakokeita, jotka eivät olleet osa tarkistettavia osakokeita",
            ),
            Validation.assertTrue(
                {
                    (it.suoritus.tarkistusarviointi?.saapumispaiva ?: LocalDate.MIN) <=
                        (it.suoritus.tarkistusarviointi?.kasittelypaiva ?: LocalDate.MAX)
                },
                path = listOf("suoritus", "tarkistusarviointi", "kasittelypaiva"),
                message = "Käsittelypäivä on ennen saapumispäivää",
            ),
        )

    fun validateKielikoodi(s: YkiHenkilosuoritus): ValidationResult<YkiHenkilosuoritus> =
        Validation.fold(
            s,
            Validation.assertTrue(
                { !it.suoritus.kieli.isLegacy() || it.suoritus.tutkintopaiva.isBefore(LocalDate.of(2017, 1, 1)) },
                listOf("suoritus", "kieli"),
                "Käytöstä poistuneita kielikoodeja (${Tutkintokieli.legacyEntries.joinToString(", ") }) ei voi käyttää",
            ),
        )

    fun validateCountryCode(s: YkiHenkilosuoritus): ValidationResult<YkiHenkilosuoritus> {
        if (s.henkilo.maa == null) return Validation.ok(s)
        val koodisto =
            localizationService
                .translationBuilder()
                .koodistot("maatjavaltiot1")
                .build()
        val maatJaValtiot = koodisto.koodistot["maatjavaltiot1"] ?: return Validation.ok(s)
        maatJaValtiot[s.henkilo.maa]
            ?: return Validation.fail(
                listOf("henkilo", "maa"),
                "Virheellinen maakoodi",
            )
        return Validation.ok(s)
    }

    fun validateArvosanat(s: YkiHenkilosuoritus): ValidationResult<YkiHenkilosuoritus> {
        val osakokeet = s.suoritus.osat.mapNotNull { if (it.arvosana == 12) null else it }
        if (osakokeet.isEmpty()) {
            return Validation.fail(
                path = listOf("suoritus", "osat"),
                message = "Suorituksella täytyy olla vähintään yksi osakoe, johon on ilmottauduttu",
            )
        }
        val validArvosanat = Koodisto.YkiArvosana.validIntegers
        val invalidArvosanat = osakokeet.mapNotNull { if (it.arvosana in validArvosanat) null else it.arvosana }
        if (invalidArvosanat.isEmpty()) return Validation.ok(s.copy(suoritus = s.suoritus.copy(osat = osakokeet)))
        return Validation.fail(
            listOf("suoritus", "osat", "arvosana"),
            "Suoritus sisältää virheellisiä arvosanoja: ${invalidArvosanat.joinToString(", ")}",
        )
    }
}
