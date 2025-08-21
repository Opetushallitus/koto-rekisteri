package fi.oph.kitu

typealias ValidationResult<T> = TypedResult<out T, Validation.ValidationException>

interface Validation<T> {
    fun validateAndEnrich(value: T): ValidationResult<T> {
        val result = validationBeforeEnrichment(value)
        if (result.isFailure) {
            return result
        }
        return validationAfterEnrichment(enrich(value))
    }

    fun validationBeforeEnrichment(value: T): ValidationResult<T> = ok(value)

    fun enrich(value: T): T = value

    fun validationAfterEnrichment(value: T): ValidationResult<T> = ok(value)

    data class ValidationException(
        val errors: List<String>,
    ) : Exception(errors.joinToString("; "))

    companion object {
        inline fun <reified T> fold(
            value: T,
            vararg validations: (T) -> ValidationResult<T>,
        ): ValidationResult<T> {
            val errors =
                validations
                    .map { it(value) }
                    .flatMap { it.errorOrNull()?.errors.orEmpty() }
            return if (errors.isNotEmpty()) {
                fail(errors)
            } else {
                ok(value)
            }
        }

        fun <T> ok(value: T): ValidationResult<T> = TypedResult.Success(value)

        fun <T> fail(reason: String): ValidationResult<T> = fail(listOf(reason))

        fun <T> fail(reasons: List<String>): ValidationResult<T> = TypedResult.Failure(ValidationException(reasons))
    }
}
