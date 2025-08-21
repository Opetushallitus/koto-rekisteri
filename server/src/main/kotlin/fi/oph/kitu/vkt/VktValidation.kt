package fi.oph.kitu.vkt

import fi.oph.kitu.Validation
import fi.oph.kitu.ValidationResult
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.vkt.tiedonsiirtoschema.OidString
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class VktValidation : Validation<VktSuoritus> {
    @Value("\${kitu.oids.palvelukayttaja}")
    lateinit var palvelukayttajaOid: String

    override fun enrich(s: VktSuoritus): VktSuoritus =
        if (s.taitotaso == Koodisto.VktTaitotaso.Erinomainen) {
            s.copy(
                suorituspaikkakunta =
                    s.suorituspaikkakunta ?: "091",
                suorituksenVastaanottaja =
                    s.suorituksenVastaanottaja ?: OidString(palvelukayttajaOid),
            )
        } else {
            s
        }

    override fun validationAfterEnrichment(value: VktSuoritus): ValidationResult<VktSuoritus> =
        Validation.fold(
            value,
            { validateSuorituspaikkakunta(it) },
            { validateSuorituksenVastaanottaja(it) },
        )

    private fun validateSuorituspaikkakunta(s: VktSuoritus): ValidationResult<VktSuoritus> =
        if (s.suorituspaikkakunta == null) {
            Validation.fail(listOf("suoritus", "suorituspaikkakunta"), "Suorituspaikkakunta puuttuu")
        } else {
            Validation.ok(s)
        }

    private fun validateSuorituksenVastaanottaja(s: VktSuoritus): ValidationResult<VktSuoritus> =
        if (s.suorituksenVastaanottaja == null) {
            Validation.fail(listOf("suoritus", "suorituksenVastaanottaja"), "Suorituksen vastaanottaja puuttuu")
        } else {
            Validation.ok(s)
        }
}
