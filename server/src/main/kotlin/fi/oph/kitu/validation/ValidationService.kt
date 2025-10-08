package fi.oph.kitu.validation

import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.VktValidation
import fi.oph.kitu.yki.YkiValidation
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import org.springframework.stereotype.Service

@Service
final class ValidationService(
    val vkt: VktValidation,
    val yki: YkiValidation,
) {
    fun validateAndEnrich(suoritus: VktSuoritus): ValidationResult<out VktSuoritus> = vkt.validateAndEnrich(suoritus)

    fun validateAndEnrich(suoritus: YkiSuoritus): ValidationResult<out YkiSuoritus> = yki.validateAndEnrich(suoritus)
}
