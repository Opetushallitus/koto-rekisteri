package fi.oph.kitu.util.validation

import arrow.core.NonEmptyList
import arrow.core.raise.either
import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.HenkilosuoritusValidation
import fi.oph.kitu.tiedonsiirtoschema.KielitutkinnonSuoritus
import fi.oph.kitu.tiedonsiirtoschema.VktSuoritus
import fi.oph.kitu.tiedonsiirtoschema.YkiSuoritus
import fi.oph.kitu.vkt.VktValidation
import fi.oph.kitu.yki.arvioijat.YkiArvioija
import fi.oph.kitu.yki.arvioijat.YkiArvioijaValidation
import fi.oph.kitu.yki.suoritukset.YkiSuoritusValidation
import org.springframework.stereotype.Service

@Service
final class ValidationService(
    val commonValidation: HenkilosuoritusValidation,
    val vkt: VktValidation,
    val ykiSuoritus: YkiSuoritusValidation,
    val ykiArvioija: YkiArvioijaValidation,
) {
    fun <T : KielitutkinnonSuoritus> validateAndEnrich(hs: Henkilosuoritus<T>): ValidationResult<Henkilosuoritus<T>> =
        either {
            val common = with(commonValidation) { validateAndEnrich(hs) }
            val enriched =
                when (val s = common.suoritus) {
                    is VktSuoritus -> {
                        with(vkt) {
                            validateAndEnrich(Henkilosuoritus(common.henkilo, s, common.lisatty))
                        }
                    }

                    is YkiSuoritus -> {
                        with(ykiSuoritus) {
                            validateAndEnrich(Henkilosuoritus(common.henkilo, s, common.lisatty))
                        }
                    }
                }
            @Suppress("UNCHECKED_CAST")
            enriched as Henkilosuoritus<T>
        }

    fun validateAndEnrich(arvioija: YkiArvioija): ValidationResult<YkiArvioija> =
        either { with(ykiArvioija) { validateAndEnrich(arvioija) } }
}

fun <T> ValidationResult<T>.getOrThrow(): T =
    fold(
        ifLeft = { errors: NonEmptyList<Validation.ValidationError> ->
            throw Validation.ValidationException(errors)
        },
        ifRight = { it },
    )
