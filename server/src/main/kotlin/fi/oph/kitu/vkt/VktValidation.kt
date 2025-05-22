package fi.oph.kitu.vkt

import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.schema.OidString
import fi.oph.kitu.schema.Validation

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

    override fun validationAfterEnrichment(value: VktSuoritus): Validation.Status =
        Validation.fold(
            value,
            { validateSuorituspaikkakunta(it) },
            { validateSuorituksenVastaanottaja(it) },
        )

    private fun validateSuorituspaikkakunta(s: VktSuoritus): Validation.Status =
        if (s.suorituspaikkakunta == null) {
            Validation.Failure(listOf("Suorituspaikkakunta puuttuu"))
        } else {
            Validation.Success()
        }

    private fun validateSuorituksenVastaanottaja(s: VktSuoritus): Validation.Status =
        if (s.suorituksenVastaanottaja == null) {
            Validation.Failure(listOf("Suorituksen vastaanottaja puuttuu"))
        } else {
            Validation.Success()
        }
}
