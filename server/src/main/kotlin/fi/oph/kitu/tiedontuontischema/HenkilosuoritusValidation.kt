package fi.oph.kitu.tiedontuontischema

import arrow.core.raise.ExperimentalRaiseAccumulateApi
import arrow.core.raise.accumulate
import arrow.core.raise.ensure
import fi.oph.kitu.oppijanumero.OppijanumeroValidation
import fi.oph.kitu.util.validation.Validation
import fi.oph.kitu.util.validation.Validation.ValidationError
import fi.oph.kitu.util.validation.ValidationRaise
import org.springframework.stereotype.Service

@Service
class HenkilosuoritusValidation(
    val onr: OppijanumeroValidation,
) : Validation<Henkilosuoritus<*>> {
    @OptIn(ExperimentalRaiseAccumulateApi::class)
    override fun ValidationRaise.validateBeforeEnrichment(value: Henkilosuoritus<*>) {
        accumulate {
            accumulating {
                ensure(value.suoritus.internalId == null) {
                    ValidationError(
                        listOf("suoritus", "internalId"),
                        "internalId on sisäinen kenttä, eikä sitä voi asettaa",
                    )
                }
            }
            accumulating {
                ensure(value.suoritus.koskiSiirtoKasitelty != true) {
                    ValidationError(
                        listOf("suoritus", "koskiSiirtoKasitelty"),
                        "koskiSiirtoKasitelty on sisäinen kenttä, eikä sitä voi asettaa arvoon true",
                    )
                }
            }
            accumulating {
                ensure(value.suoritus.koskiOpiskeluoikeusOid == null) {
                    ValidationError(
                        listOf("suoritus", "koskiOpiskeluoikeusOid"),
                        "koskiOpiskeluoikeusOid on sisäinen kenttä, eikä sitä voi asettaa",
                    )
                }
            }
            accumulating { with(onr) { validateOppijanumero(value.henkilo.oid, listOf("henkilo", "oid")) } }
        }
    }
}
