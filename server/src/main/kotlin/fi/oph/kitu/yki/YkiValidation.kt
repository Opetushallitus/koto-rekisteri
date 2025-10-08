package fi.oph.kitu.yki

import fi.oph.kitu.intersects
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.organisaatiot.OrganisaatioService
import fi.oph.kitu.organisaatiot.OrganisaatiopalveluException
import fi.oph.kitu.validation.Validation
import fi.oph.kitu.validation.ValidationResult
import fi.oph.kitu.yki.suoritukset.YkiHenkilosuoritus
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class YkiValidation(
    val organisaatiot: OrganisaatioService,
) : Validation<YkiHenkilosuoritus> {
    override fun validationBeforeEnrichment(suoritus: YkiHenkilosuoritus): ValidationResult<YkiHenkilosuoritus> =
        validateOrganisaatiot(suoritus)

    fun validateOrganisaatiot(s: YkiHenkilosuoritus): ValidationResult<YkiHenkilosuoritus> {
        val suoritus = s.suoritus
        val sallitutOrganisaatiotyypit =
            listOf(
                Koodisto.Organisaatiotyyppi.Oppilaitos,
                Koodisto.Organisaatiotyyppi.Toimipiste,
            )

        val oid = suoritus.jarjestaja.oid

        fun fail(reason: String): ValidationResult<YkiHenkilosuoritus> =
            Validation.fail(
                listOf("suoritus", "jarjestaja", "oid"),
                reason,
            )

        return organisaatiot.getOrganisaatio(oid).fold(
            onSuccess = { org ->
                val tyypit = org.tyypit.mapNotNull { Koodisto.Organisaatiotyyppi.of(it) }
                if (tyypit.intersects(sallitutOrganisaatiotyypit)) {
                    Validation.ok(s)
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

    companion object {
        // Tästä päivästä alkaen yki-suorituksille ei siirrettä henkilötunnusta
        val hetunSiirronRajapaiva = LocalDate.of(2026, 1, 1)
    }
}
