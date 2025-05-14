package fi.oph.kitu.oppijanumero

open class OppijanumeroException(
    val request: YleistunnisteHaeRequest,
    message: String,
    val oppijanumeroServiceError: OppijanumeroServiceError? = null,
    cause: Throwable? = null,
) : Throwable(message, cause) {
    class BadRequestToOppijanumero(
        request: YleistunnisteHaeRequest,
        message: String = "Bad Request to oppijanumero",
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
