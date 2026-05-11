package fi.oph.kitu.util.validation

import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
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
    ): ValidationResult<Henkilosuoritus<T>> =
        either {
            with(commonValidation) { validateAndEnrich(hs) }
            @Suppress("UNCHECKED_CAST")
            when (hs.suoritus) {
                is VktSuoritus -> {
                    with(vkt) {
                        validateAndEnrich(Henkilosuoritus(hs.henkilo, hs.suoritus))
                    } as Henkilosuoritus<T>
                }

                is YkiSuoritus -> {
                    with(ykiSuoritus) {
                        validateAndEnrich(Henkilosuoritus(hs.henkilo, hs.suoritus))
                    } as Henkilosuoritus<T>
                }

                else -> {
                    raise(
                        nonEmptyListOf(
                            Validation.ValidationError(
                                emptyList(),
                                "Validation not implemented for ${hs::class.simpleName}",
                            ),
                        ),
                    )
                }
            }
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
