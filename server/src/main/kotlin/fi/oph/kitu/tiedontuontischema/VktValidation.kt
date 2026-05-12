package fi.oph.kitu.tiedontuontischema

import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import arrow.core.raise.zipOrAccumulate
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.util.validation.Validation
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
                        it.suorituksenVastaanottaja ?: Oid.Companion.parse(palvelukayttajaOid).getOrNull(),
                )
            }
        } else {
            value
        }

    override fun Raise<NonEmptyList<Validation.ValidationError>>.validateAfterEnrichment(
        value: VktHenkilosuoritus,
    ): VktHenkilosuoritus {
        zipOrAccumulate(
            {
                ensureNotNull(value.suoritus.suorituspaikkakunta) {
                    Validation.ValidationError(listOf("suoritus", "suorituspaikkakunta"), "Suorituspaikkakunta puuttuu")
                }
            },
            {
                ensureNotNull(value.suoritus.suorituksenVastaanottaja) {
                    Validation.ValidationError(
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
                    Validation.ValidationError(
                        listOf("suoritus", "osakokeet", "arviointi"),
                        "Suorituksella on arvioimattomia osakokeita",
                    )
                }
            },
        ) { _, _, _ -> }
        return value
    }
}
