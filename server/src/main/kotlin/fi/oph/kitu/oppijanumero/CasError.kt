package fi.oph.kitu.oppijanumero

sealed class CasError(
    message: String,
) : Throwable(message) {
    class GrantingTicketError(
        message: String,
    ) : CasError(message)

    class ServiceTicketError(
        message: String,
    ) : CasError(message)
}
