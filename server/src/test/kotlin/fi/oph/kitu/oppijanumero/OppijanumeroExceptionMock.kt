package fi.oph.kitu.oppijanumero

class OppijanumeroExceptionMock(
    request: YleistunnisteHaeRequest,
    message: String = "OppijanumeroServiceMock",
    oppijanumeroServiceError: OppijanumeroServiceError? = null,
    cause: Throwable? = null,
) : OppijanumeroException(
        request = request,
        message = message,
        oppijanumeroServiceError = oppijanumeroServiceError,
        cause = cause,
    )
