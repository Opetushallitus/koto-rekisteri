package fi.oph.kitu.vkt

import fi.oph.kitu.Validation
import fi.oph.kitu.ValidationResult
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.schema.OidString

object VktValidation : Validation<VktSuoritus> {
    override fun enrich(s: VktSuoritus): VktSuoritus =
        if (s.taitotaso == Koodisto.VktTaitotaso.Erinomainen) {
            s.copy(
                suorituspaikkakunta = s.suorituspaikkakunta ?: "091",
                suorituksenVastaanottaja =
                    s.suorituksenVastaanottaja ?: OidString(
                        "1.2.246.562.24.77101904300",
                    ),
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
            Validation.fail("Suorituspaikkakunta puuttuu")
        } else {
            Validation.ok(s)
        }

    private fun validateSuorituksenVastaanottaja(s: VktSuoritus): ValidationResult<VktSuoritus> =
        if (s.suorituksenVastaanottaja == null) {
            Validation.fail("Suorituksen vastaanottaja puuttuu")
        } else {
            Validation.ok(s)
        }
}
