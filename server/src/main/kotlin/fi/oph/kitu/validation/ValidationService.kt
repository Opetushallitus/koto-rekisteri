package fi.oph.kitu.validation

import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.HenkilosuoritusValidation
import fi.oph.kitu.tiedonsiirtoschema.KielitutkinnonSuoritus
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.VktValidation
import fi.oph.kitu.yki.YkiValidation
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import org.springframework.stereotype.Service

@Service
final class ValidationService(
    val commonValidation: HenkilosuoritusValidation,
    val vkt: VktValidation,
    val yki: YkiValidation,
) {
    inline fun <reified T : KielitutkinnonSuoritus> validateAndEnrich(
        hs: Henkilosuoritus<T>,
    ): ValidationResult<out Henkilosuoritus<T>> {
        val result =
            commonValidation.validateAndEnrich(hs).flatMap {
                when (hs.suoritus) {
                    is VktSuoritus -> vkt.validateAndEnrich(Henkilosuoritus(hs.henkilo, hs.suoritus))
                    is YkiSuoritus -> yki.validateAndEnrich(Henkilosuoritus(hs.henkilo, hs.suoritus))
                    else -> throw IllegalStateException("Validation not implemented for ${hs::class.simpleName}")
                }
            }

        @Suppress("UNCHECKED_CAST")
        return result as ValidationResult<out Henkilosuoritus<T>>
    }
}
