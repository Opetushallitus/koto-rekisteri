package fi.oph.kitu.oppijanumero

import java.net.http.HttpResponse

open class OppijanumeroException(
    val request: YleistunnisteHaeRequest,
    message: String,
    val oppijanumeroServiceError: OppijanumeroServiceError? = null,
    cause: Throwable? = null,
) : Throwable(message, cause) {
    class UnexpectedError(
        request: YleistunnisteHaeRequest,
        val response: HttpResponse<String>,
        message: String = "Unexpected error in oppijanumero-service",
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
