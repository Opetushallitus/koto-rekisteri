package fi.oph.kitu.util.validation

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.Raise

typealias ValidationResult<T> = Either<NonEmptyList<Validation.ValidationError>, T>

typealias ValidationRaise = Raise<NonEmptyList<Validation.ValidationError>>

interface Validation<T> {
    fun ValidationRaise.validateAndEnrich(value: T): T {
        validateBeforeEnrichment(value)
        val enriched = enrich(value)
        validateAfterEnrichment(enriched)
        return enriched
    }

    fun ValidationRaise.validateBeforeEnrichment(value: T) {}

    fun enrich(value: T): T = value

    fun ValidationRaise.validateAfterEnrichment(value: T) {}

    data class ValidationException(
        val errors: NonEmptyList<ValidationError>,
    ) : Exception(errors.joinToString("; "))

    data class ValidationError(
        val path: List<String>,
        val message: String,
    ) {
        override fun toString(): String = "${path.joinToString(".")}: $message"
    }
}
