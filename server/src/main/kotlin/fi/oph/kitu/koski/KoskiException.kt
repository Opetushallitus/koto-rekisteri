package fi.oph.kitu.koski

class KoskiException(
    val suoritusId: String,
    message: String?,
) : Throwable(message)
