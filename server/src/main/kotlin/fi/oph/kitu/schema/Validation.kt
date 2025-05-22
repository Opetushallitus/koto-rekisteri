package fi.oph.kitu.schema

interface Validation<T> {
    fun validateAndEnrich(value: T): Result<T> {
        val result = validationBeforeEnrichment(value)
        if (result is Failure) {
            return result.toResult(value)
        }
        val enriched = enrich(value)
        val result2 = validationAfterEnrichment(enriched)
        return result2.toResult(enriched)
    }

    fun validationBeforeEnrichment(value: T): Status = Success()

    fun enrich(value: T): T = value

    fun validationAfterEnrichment(value: T): Status = Success()

    interface Status {
        fun <T> toResult(value: T): Result<T>
    }

    class Success : Status {
        override fun <T> toResult(value: T): Result<T> = Result.success(value)
    }

    data class Failure(
        val errors: List<String>,
    ) : Status {
        override fun <T> toResult(value: T): Result<T> = Result.failure(ValidationException(errors))
    }

    data class ValidationException(
        val errors: List<String>,
    ) : Exception(errors.joinToString("\n"))

    companion object {
        inline fun <reified T> fold(
            value: T,
            vararg validations: (T) -> Status,
        ): Status {
            val errors =
                validations
                    .map { it(value) }
                    .filterIsInstance<Failure>()
                    .flatMap { it.errors }
            return if (errors.isNotEmpty()) {
                Failure(errors)
            } else {
                Success()
            }
        }
    }
}
