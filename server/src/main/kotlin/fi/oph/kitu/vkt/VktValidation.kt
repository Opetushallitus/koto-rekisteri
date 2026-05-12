package fi.oph.kitu.vkt

import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.raise.zipOrAccumulate
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.tiedontuontischema.VktHenkilosuoritus
import fi.oph.kitu.util.validation.Validation
import fi.oph.kitu.util.validation.Validation.ValidationError
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

    override fun Raise<NonEmptyList<ValidationError>>.validateAfterEnrichment(
        value: VktHenkilosuoritus,
    ): VktHenkilosuoritus {
        zipOrAccumulate(
            {
                ensureNotNull(value.suoritus.suorituspaikkakunta) {
                    ValidationError(listOf("suoritus", "suorituspaikkakunta"), "Suorituspaikkakunta puuttuu")
                }
            },
            {
                ensureNotNull(value.suoritus.suorituksenVastaanottaja) {
                    ValidationError(
                        listOf("suoritus", "suorituksenVastaanottaja"),
                        "Suorituksen vastaanottaja puuttuu",
                    )
                }
            },
            {
                ensure(
                    value.suoritus.taitotaso != Koodisto.VktTaitotaso.HyväJaTyydyttävä ||
                        value.suoritus.osat.all { it.arviointi != null },
                ) {
                    ValidationError(
                        listOf("suoritus", "osakokeet", "arviointi"),
                        "Suorituksella on arvioimattomia osakokeita",
                    )
                }
            },
        ) { _, _, _ -> }
        return value
    }
}
