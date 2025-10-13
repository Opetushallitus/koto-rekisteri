package fi.oph.kitu.validation

typealias ValidationResult<T> = fi.oph.kitu.TypedResult<out T, Validation.ValidationException>

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
        val errors: List<ValidationError>,
    ) : Exception(errors.joinToString("; "))

    data class ValidationError(
        val path: List<String>,
        val message: String,
    ) {
        override fun toString(): String = "${path.joinToString(".")}: $message"
    }

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

        fun <T> ok(value: T): ValidationResult<T> =
            fi.oph.kitu.TypedResult
                .Success(value)

        fun <T> fail(
            path: List<String>,
            message: String,
        ): ValidationResult<T> = fail(listOf(ValidationError(path, message)))

        fun <T> fail(reasons: List<ValidationError>): ValidationResult<T> =
            fi.oph.kitu.TypedResult
                .Failure(ValidationException(reasons))

        fun <T, A> assert(
            getActual: (T) -> A,
            path: List<String>,
            message: String,
            isOk: (A) -> Boolean,
        ): (T) -> ValidationResult<T> =
            {
                if (isOk(getActual(it))) {
                    ok(it)
                } else {
                    fail(path, message)
                }
            }

        fun <T, A> assertEquals(
            expected: A,
            getActual: (T) -> A,
            path: List<String>,
            message: String,
        ): (T) -> ValidationResult<T> = assert(getActual, path, message) { it == expected }

        fun <T, A> assertNotEquals(
            expected: A,
            getActual: (T) -> A,
            path: List<String>,
            message: String,
        ): (T) -> ValidationResult<T> = assert(getActual, path, message) { it != expected }
    }
}
