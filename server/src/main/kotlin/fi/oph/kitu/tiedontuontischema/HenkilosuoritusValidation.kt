package fi.oph.kitu.tiedontuontischema

import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.accumulate
import arrow.core.raise.ensure
import fi.oph.kitu.koodisto.KoodistoService
import fi.oph.kitu.oppijanumero.OppijanumeroValidation
import fi.oph.kitu.util.validation.EnrichmentRaise
import fi.oph.kitu.util.validation.Validation
import fi.oph.kitu.util.validation.Validation.ValidationError
import fi.oph.kitu.util.validation.ValidationRaise
import org.springframework.stereotype.Service

@Service
class HenkilosuoritusValidation(
    val onr: OppijanumeroValidation,
    val koodistot: KoodistoService,
) : Validation<Henkilosuoritus<*>> {
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
                ensure(!value.suoritus.koskiSiirtoKasitelty) {
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

    override fun EnrichmentRaise.enrich(value: Henkilosuoritus<*>): Henkilosuoritus<*> =
        value.copy(henkilo = value.henkilo.copy(maa = value.henkilo.maa?.uppercase()))

    override fun ValidationRaise.validateAfterEnrichment(value: Henkilosuoritus<*>) {
        accumulate {
            accumulating {
                validateMaa(value)
            }
        }
    }

    private fun ValidationRaise.validateMaa(s: Henkilosuoritus<*>) {
        if (s.henkilo.maa == null) return
        val maa =
            koodistot
                .getKoodiviitteet("maatjavaltiot1")
                ?.find { it.koodiArvo.equals(s.henkilo.maa, ignoreCase = true) }

        ensure(maa != null) {
            NonEmptyList.of(
                listOf(
                    ValidationError(
                        listOf("henkilo", "maa"),
                        "Virheellinen maakoodi",
                    ),
                ),
            )
        }
    }
}
