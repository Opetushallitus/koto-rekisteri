package fi.oph.kitu.koski

class KoskiException(
    val suoritusId: Int?,
    message: String?,
) : Throwable(message)
