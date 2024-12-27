package fi.oph.kitu.oppijanumero

class OppijanumeroException(
    val oppija: Oppija,
    message: String,
    val oppijanumeroServiceError: OppijanumeroServiceError? = null,
    cause: Throwable? = null,
) : Throwable(message, cause)
