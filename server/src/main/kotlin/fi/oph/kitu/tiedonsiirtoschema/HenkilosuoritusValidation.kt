package fi.oph.kitu.tiedonsiirtoschema

import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import fi.oph.kitu.oppijanumero.OppijanumeroValidation
import fi.oph.kitu.util.validation.Validation
import fi.oph.kitu.util.validation.Validation.ValidationError
import org.springframework.stereotype.Service

@Service
class HenkilosuoritusValidation(
    val onr: OppijanumeroValidation,
) : Validation<Henkilosuoritus<*>> {
    override fun Raise<NonEmptyList<ValidationError>>.validateBeforeEnrichment(
        value: Henkilosuoritus<*>,
    ): Henkilosuoritus<*> {
        zipOrAccumulate(
            {
                ensure(value.suoritus.internalId == null) {
                    ValidationError(
                        listOf("suoritus", "internalId"),
                        "internalId on sisäinen kenttä, eikä sitä voi asettaa",
                    )
                }
            },
            {
                ensure(value.suoritus.koskiSiirtoKasitelty != true) {
                    ValidationError(
                        listOf("suoritus", "koskiSiirtoKasitelty"),
                        "koskiSiirtoKasitelty on sisäinen kenttä, eikä sitä voi asettaa arvoon true",
                    )
                }
            },
            {
                ensure(value.suoritus.koskiOpiskeluoikeusOid == null) {
                    ValidationError(
                        listOf("suoritus", "koskiOpiskeluoikeusOid"),
                        "koskiOpiskeluoikeusOid on sisäinen kenttä, eikä sitä voi asettaa",
                    )
                }
            },
            { with(onr) { validateOppijanumero(value.henkilo.oid, listOf("henkilo", "oid")) } },
        ) { _, _, _, _ -> }
        return value
    }
}
