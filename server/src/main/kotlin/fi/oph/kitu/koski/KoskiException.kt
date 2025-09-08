package fi.oph.kitu.koski

class KoskiException(
    val suoritusId: KoskiErrorMappingId,
    message: String?,
) : Throwable(message)
