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

    class VerifyTicketError(
        message: String,
    ) : CasError(message)

    class CasAuthServiceError(
        message: String,
    ) : CasError(message)
}
