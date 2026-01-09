package fi.oph.kitu.ilmoittautumisjarjestelma

import fi.oph.kitu.defaultObjectMapper
import org.springframework.http.ResponseEntity

sealed class IlmoittautumisjarjestelmaException(
    val request: IlmoittautumisjarjestelmaRequest,
    val response: ResponseEntity<String>?,
    message: String,
    cause: Throwable? = null,
) : Throwable(message, cause) {
    class BadRequest(
        request: IlmoittautumisjarjestelmaRequest,
        response: ResponseEntity<String>,
        message: String = "Bad request",
        cause: Throwable? = null,
    ) : IlmoittautumisjarjestelmaException(request, response, message, cause)

    class UnexpectedError(
        request: IlmoittautumisjarjestelmaRequest,
        response: ResponseEntity<String>,
        cause: Throwable? = null,
    ) : IlmoittautumisjarjestelmaException(request, response, "Unexpected error", cause)

    class MalformedResponse(
        request: IlmoittautumisjarjestelmaRequest,
        response: ResponseEntity<String>,
        cause: Throwable? = null,
    ) : IlmoittautumisjarjestelmaException(request, response, "Malformed response", cause)

    fun debugString(): String =
        listOfNotNull(
            message,
            "request: ${defaultObjectMapper.writeValueAsString(request)}",
            response?.statusCode?.let { "response status: $it" },
            response?.body?.let { "response body: $it" },
            cause?.let { "cause: $it" },
        ).joinToString("; ")
}
