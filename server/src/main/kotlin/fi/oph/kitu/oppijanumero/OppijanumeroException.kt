package fi.oph.kitu.oppijanumero

class OppijanumeroException(
    val oppija: Oppija,
    message: String,
    cause: Throwable? = null,
) : Throwable(message, cause)
