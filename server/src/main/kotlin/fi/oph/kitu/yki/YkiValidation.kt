package fi.oph.kitu.yki

import fi.oph.kitu.intersects
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.organisaatiot.OrganisaatioService
import fi.oph.kitu.organisaatiot.OrganisaatiopalveluException
import fi.oph.kitu.validation.Validation
import fi.oph.kitu.validation.ValidationResult
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import org.springframework.stereotype.Service

@Service
class YkiValidation(
    val organisaatiot: OrganisaatioService,
) : Validation<YkiSuoritus> {
    override fun validationBeforeEnrichment(suoritus: YkiSuoritus): ValidationResult<YkiSuoritus> =
        validateOrganisaatiot(suoritus)

    private fun validateOrganisaatiot(suoritus: YkiSuoritus): ValidationResult<YkiSuoritus> {
        val sallitutOrganisaatiotyypit =
            listOf(
                Koodisto.Organisaatiotyyppi.Oppilaitos,
                Koodisto.Organisaatiotyyppi.Toimipiste,
            )

        val oid = suoritus.jarjestaja.oid

        fun fail(reason: String): ValidationResult<YkiSuoritus> =
            Validation.fail(
                listOf("jarjestaja.oid"),
                reason,
            )

        return organisaatiot.getOrganisaatio(oid).fold(
            onSuccess = { org ->
                val tyypit = org.tyypit.mapNotNull { Koodisto.Organisaatiotyyppi.of(it) }
                if (tyypit.intersects(sallitutOrganisaatiotyypit)) {
                    Validation.ok(suoritus)
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
