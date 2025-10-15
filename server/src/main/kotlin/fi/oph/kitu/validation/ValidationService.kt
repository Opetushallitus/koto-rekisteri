package fi.oph.kitu.validation

import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.HenkilosuoritusValidation
import fi.oph.kitu.tiedonsiirtoschema.KielitutkinnonSuoritus
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.VktValidation
import fi.oph.kitu.yki.arvioijat.YkiArvioija
import fi.oph.kitu.yki.arvioijat.YkiArvioijaValidation
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import fi.oph.kitu.yki.suoritukset.YkiSuoritusValidation
import org.springframework.stereotype.Service

@Service
final class ValidationService(
    val commonValidation: HenkilosuoritusValidation,
    val vkt: VktValidation,
    val ykiSuoritus: YkiSuoritusValidation,
    val ykiArvioija: YkiArvioijaValidation,
) {
    inline fun <reified T : KielitutkinnonSuoritus> validateAndEnrich(
        hs: Henkilosuoritus<T>,
    ): ValidationResult<out Henkilosuoritus<T>> {
        val result =
            commonValidation.validateAndEnrich(hs).flatMap {
                when (hs.suoritus) {
                    is VktSuoritus -> vkt.validateAndEnrich(Henkilosuoritus(hs.henkilo, hs.suoritus))
                    is YkiSuoritus -> ykiSuoritus.validateAndEnrich(Henkilosuoritus(hs.henkilo, hs.suoritus))
                    else -> throw IllegalStateException("Validation not implemented for ${hs::class.simpleName}")
                }
            }

        @Suppress("UNCHECKED_CAST")
        return result as ValidationResult<out Henkilosuoritus<T>>
    }

    fun validateAndEnrich(arvioija: YkiArvioija): ValidationResult<out YkiArvioija> =
        ykiArvioija.validateAndEnrich(arvioija)
}
