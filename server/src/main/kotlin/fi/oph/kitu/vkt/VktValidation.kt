package fi.oph.kitu.vkt

import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.util.validation.Validation
import fi.oph.kitu.util.validation.ValidationResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class VktValidation : Validation<VktHenkilosuoritus> {
    @Value("\${kitu.oids.palvelukayttaja}")
    lateinit var palvelukayttajaOid: String

    override fun enrich(value: VktHenkilosuoritus): VktHenkilosuoritus =
        if (value.suoritus.taitotaso == Koodisto.VktTaitotaso.Erinomainen) {
            value.modifySuoritus {
                it.copy(
                    suorituspaikkakunta =
                        it.suorituspaikkakunta ?: "091",
                    suorituksenVastaanottaja =
                        it.suorituksenVastaanottaja ?: Oid.parse(palvelukayttajaOid).getOrNull(),
                )
            }
        } else {
            value
        }

    override fun validationAfterEnrichment(value: VktHenkilosuoritus): ValidationResult<VktHenkilosuoritus> =
        Validation.fold(
            value,
            { validateSuorituspaikkakunta(it) },
            { validateSuorituksenVastaanottaja(it) },
            { validateOsasuoritustenArviointi(it) },
        )

    private fun validateSuorituspaikkakunta(s: VktHenkilosuoritus): ValidationResult<VktHenkilosuoritus> =
        if (s.suoritus.suorituspaikkakunta == null) {
            Validation.fail(listOf("suoritus", "suorituspaikkakunta"), "Suorituspaikkakunta puuttuu")
        } else {
            Validation.ok(s)
        }

    private fun validateSuorituksenVastaanottaja(s: VktHenkilosuoritus): ValidationResult<VktHenkilosuoritus> =
        if (s.suoritus.suorituksenVastaanottaja == null) {
            Validation.fail(listOf("suoritus", "suorituksenVastaanottaja"), "Suorituksen vastaanottaja puuttuu")
        } else {
            Validation.ok(s)
        }

    private fun validateOsasuoritustenArviointi(s: VktHenkilosuoritus): ValidationResult<VktHenkilosuoritus> =
        if (s.suoritus.taitotaso == Koodisto.VktTaitotaso.HyväJaTyydyttävä &&
            s.suoritus.osat.any { it.arviointi == null }
        ) {
            Validation.fail(listOf("suoritus", "osakokeet", "arviointi"), "Suorituksella on arvioimattomia osakokeita")
        } else {
            Validation.ok(s)
        }
}
