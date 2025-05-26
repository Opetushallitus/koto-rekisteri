package fi.oph.kitu.oppijanumero

import org.springframework.http.ResponseEntity

sealed class OppijanumeroException(
    val request: YleistunnisteHaeRequest,
    message: String,
    val oppijanumeroServiceError: OppijanumeroServiceError? = null,
    cause: Throwable? = null,
) : Throwable(message, cause) {
    class UnexpectedError(
        request: YleistunnisteHaeRequest,
        val response: ResponseEntity<String>,
        message: String = "Unexpected error in oppijanumero-service",
    ) : OppijanumeroException(request, message)

    class MalformedResponse(
        request: YleistunnisteHaeRequest,
        val response: HttpResponse<String>,
        message: String = "Malformed response from oppijanumero-service",
        cause: Throwable,
    ) : OppijanumeroException(request, message, cause = cause)

    class BadResponse(
        request: YleistunnisteHaeRequest,
        val response: HttpResponse<String>,
        message: String = "Bad response from oppijanumero-service",
        oppijanumeroServiceError: OppijanumeroServiceError,
        cause: Throwable,
    ) : OppijanumeroException(request, message, oppijanumeroServiceError, cause)

    class BadRequest(
        request: YleistunnisteHaeRequest,
        val response: ResponseEntity<String>,
        message: String = "Bad request to oppijanumero-service",
        oppijanumeroServiceError: OppijanumeroServiceError? = null,
        cause: Throwable? = null,
    ) : OppijanumeroException(request, message, oppijanumeroServiceError, cause)

    class OppijaNotFoundException(
        request: YleistunnisteHaeRequest,
        message: String = "Oppija not found from oppijanumero-service",
        oppijanumeroServiceError: OppijanumeroServiceError? = null,
        cause: Throwable? = null,
    ) : OppijanumeroException(request, message, oppijanumeroServiceError, cause)

    class OppijaNotIdentifiedException(
        request: YleistunnisteHaeRequest,
        message: String = "Oppija $request is not identified in oppijanumero service",
        oppijanumeroServiceError: OppijanumeroServiceError? = null,
        cause: Throwable? = null,
    ) : OppijanumeroException(request, message, oppijanumeroServiceError, cause)

    class MalformedOppijanumero(
        request: YleistunnisteHaeRequest,
        oppijanumero: String?,
        message: String = "Received a malformed oppijanumero \"$oppijanumero\" for $request",
        oppijanumeroServiceError: OppijanumeroServiceError? = null,
        cause: Throwable? = null,
    ) : OppijanumeroException(request, message, oppijanumeroServiceError, cause)
}
