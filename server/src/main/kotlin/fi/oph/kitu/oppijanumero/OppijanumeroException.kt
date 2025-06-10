package fi.oph.kitu.oppijanumero

import org.springframework.http.ResponseEntity

sealed class OppijanumeroException(
    val request: OppijanumerorekisteriRequest,
    message: String,
    val oppijanumeroServiceError: OppijanumeroServiceError? = null,
    cause: Throwable? = null,
) : Throwable(message, cause) {
    class UnexpectedError(
        request: OppijanumerorekisteriRequest,
        val response: ResponseEntity<String>,
        message: String = "Unexpected error in oppijanumero-service",
    ) : OppijanumeroException(request, message)

    class MalformedResponse(
        request: OppijanumerorekisteriRequest,
        val response: ResponseEntity<String>,
        message: String = "Malformed response from oppijanumero-service",
        cause: Throwable,
    ) : OppijanumeroException(request, message, cause = cause)

    class BadResponse(
        request: OppijanumerorekisteriRequest,
        val response: ResponseEntity<String>,
        message: String = "Bad response from oppijanumero-service",
        oppijanumeroServiceError: OppijanumeroServiceError,
        cause: Throwable,
    ) : OppijanumeroException(request, message, oppijanumeroServiceError, cause)

    class BadRequest(
        request: OppijanumerorekisteriRequest,
        val response: ResponseEntity<String>,
        message: String = "Bad request to oppijanumero-service",
        oppijanumeroServiceError: OppijanumeroServiceError? = null,
        cause: Throwable? = null,
    ) : OppijanumeroException(request, message, oppijanumeroServiceError, cause)

    class OppijaNotFoundException(
        request: OppijanumerorekisteriRequest,
        message: String = "Oppija not found from oppijanumero-service",
        oppijanumeroServiceError: OppijanumeroServiceError? = null,
        cause: Throwable? = null,
    ) : OppijanumeroException(request, message, oppijanumeroServiceError, cause)

    class OppijaNotIdentifiedException(
        request: OppijanumerorekisteriRequest,
        message: String = "Oppija $request is not identified in oppijanumero service",
        oppijanumeroServiceError: OppijanumeroServiceError? = null,
        cause: Throwable? = null,
    ) : OppijanumeroException(request, message, oppijanumeroServiceError, cause)

    class MalformedOppijanumero(
        request: OppijanumerorekisteriRequest,
        oppijanumero: String?,
        message: String = "Received a malformed oppijanumero \"$oppijanumero\" for $request",
        oppijanumeroServiceError: OppijanumeroServiceError? = null,
        cause: Throwable? = null,
    ) : OppijanumeroException(request, message, oppijanumeroServiceError, cause)
}
