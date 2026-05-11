package fi.oph.kitu.util.validation

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.raise.Raise

typealias ValidationResult<T> = Either<NonEmptyList<Validation.ValidationError>, T>

interface Validation<T> {
    fun Raise<NonEmptyList<ValidationError>>.validateAndEnrich(value: T): T {
        val pre = validateBeforeEnrichment(value)
        val enriched = enrich(pre)
        return validateAfterEnrichment(enriched)
    }

    fun Raise<NonEmptyList<ValidationError>>.validateBeforeEnrichment(value: T): T = value

    fun enrich(value: T): T = value

    fun Raise<NonEmptyList<ValidationError>>.validateAfterEnrichment(value: T): T = value

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
