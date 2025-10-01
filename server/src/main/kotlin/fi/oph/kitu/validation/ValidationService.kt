package fi.oph.kitu.validation

import fi.oph.kitu.TypedResult
import fi.oph.kitu.tiedonsiirtoschema.KielitutkinnonSuoritus
import fi.oph.kitu.vkt.VktSuoritus
import fi.oph.kitu.vkt.VktValidation
import org.springframework.stereotype.Service

@Service
class ValidationService(
    private val vkt: VktValidation,
) {
    fun validateAndEnrich(
        suoritus: KielitutkinnonSuoritus,
    ): TypedResult<out VktSuoritus, Validation.ValidationException> =
        when (suoritus) {
            is VktSuoritus -> vkt.validateAndEnrich(suoritus)
            else -> TODO("Validation for ${suoritus.tyyppi} no implemented")
        }
}
