package fi.oph.kitu.oppijanumero

open class OppijanumeroException(
    val oppija: Oppija,
    message: String,
    val oppijanumeroServiceError: OppijanumeroServiceError? = null,
    cause: Throwable? = null,
) : Throwable(message, cause) {
    class OppijaNotFoundException(
        oppija: Oppija,
        message: String = "Oppija not found from oppijanumero-service",
        oppijanumeroServiceError: OppijanumeroServiceError? = null,
        cause: Throwable? = null,
    ) : OppijanumeroException(oppija, message, oppijanumeroServiceError, cause)

    class OppijaNotIdentifiedException(
        oppija: Oppija,
        message: String = "Oppija with oid ${oppija.henkilo_oid} is not identified in oppijanumero-service",
        oppijanumeroServiceError: OppijanumeroServiceError? = null,
        cause: Throwable? = null,
    ) : OppijanumeroException(oppija, message, oppijanumeroServiceError, cause)
}
