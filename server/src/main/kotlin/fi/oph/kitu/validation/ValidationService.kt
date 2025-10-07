package fi.oph.kitu.validation

import fi.oph.kitu.TypedResult
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.VktValidation
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import org.springframework.stereotype.Service

@Service
final class ValidationService(
    val vkt: VktValidation,
) {
    fun validateAndEnrich(suoritus: VktSuoritus): ValidationResult<out VktSuoritus> = vkt.validateAndEnrich(suoritus)

    fun validateAndEnrich(suoritus: YkiSuoritus): ValidationResult<out YkiSuoritus> =
        TypedResult.Success(suoritus) // TODO: Lisää validointi
}
