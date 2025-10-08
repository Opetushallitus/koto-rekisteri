package fi.oph.kitu.organisaatiot

import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.ResponseEntity
import java.util.Date

sealed class OrganisaatiopalveluException(
    val request: OrganisaatiopalveluRequest,
    message: String,
    val organisaatiopalveluError: OrganisaatiopalveluError? = null,
    cause: Throwable? = null,
) : Throwable(message, cause) {
    class UnexpectedError(
        request: OrganisaatiopalveluRequest,
        val response: ResponseEntity<String>,
        message: String = "Unexpected error in organisaatio-service",
    ) : OrganisaatiopalveluException(request, message)

    class MalformedResponse(
        request: OrganisaatiopalveluRequest,
        val response: ResponseEntity<String>,
        message: String = "Malformed response from organisaatio-service",
        cause: Throwable,
    ) : OrganisaatiopalveluException(request, message, cause = cause)

    class BadResponse(
        request: OrganisaatiopalveluRequest,
        val response: ResponseEntity<String>,
        message: String = "Bad response from organisaatio-service",
        organisaatiopalveluError: OrganisaatiopalveluError,
        cause: Throwable,
    ) : OrganisaatiopalveluException(request, message, organisaatiopalveluError, cause)

    class BadRequest(
        request: OrganisaatiopalveluRequest,
        val response: ResponseEntity<String>,
        message: String = "Bad request to organisaatio-service",
        organisaatiopalveluError: OrganisaatiopalveluError? = null,
        cause: Throwable? = null,
    ) : OrganisaatiopalveluException(request, message, organisaatiopalveluError, cause)

    class NotFoundException(
        request: OrganisaatiopalveluRequest,
        message: String = "Oppija not found from organisaatio-service",
        organisaatiopalveluError: OrganisaatiopalveluError? = null,
        cause: Throwable? = null,
    ) : OrganisaatiopalveluException(request, message, organisaatiopalveluError, cause)

    class MalformedOid(
        request: OrganisaatiopalveluRequest,
        oppijanumero: String?,
        message: String = "Received a malformed organisaatio oid \"$oppijanumero\" for $request",
        organisaatiopalveluError: OrganisaatiopalveluError? = null,
        cause: Throwable? = null,
    ) : OrganisaatiopalveluException(request, message, organisaatiopalveluError, cause)
}

data class OrganisaatiopalveluError(
    @param:JsonProperty("timestamp")
    val timestamp: Date,
    @param:JsonProperty("status")
    val status: Int,
    @param:JsonProperty("error")
    val error: String,
    @param:JsonProperty("path")
    val path: String,
)
