package fi.oph.kitu.yki.suoritukset

import fi.oph.kitu.i18n.finnishDate
import fi.oph.kitu.intersects
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.organisaatiot.OrganisaatioService
import fi.oph.kitu.organisaatiot.OrganisaatiopalveluException
import fi.oph.kitu.validation.Validation
import fi.oph.kitu.validation.ValidationResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class YkiSuoritusValidation(
    val organisaatiot: OrganisaatioService,
    @param:Value("\${kitu.validaatiot.yki.hetunSiirronRajapaiva}")
    val hetunSiirronRajapaiva: LocalDate,
) : Validation<YkiHenkilosuoritus> {
    override fun validationBeforeEnrichment(value: YkiHenkilosuoritus): ValidationResult<YkiHenkilosuoritus> =
        Validation.fold(
            value,
            { validateOrganisaatiot(it) },
            { validateHetu(it) },
        )

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
                        is OrganisaatiopalveluException.NotFoundException ->
                            "Organisaatiota ${suoritus.jarjestaja.oid} ei löydy organisaatiopalvelusta"
                        else ->
                            "Organisaation validointi epäonnistui"
                    },
                )
            },
        )
    }
}
