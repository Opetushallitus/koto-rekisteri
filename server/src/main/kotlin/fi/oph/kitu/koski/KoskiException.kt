package fi.oph.kitu.koski

import org.springframework.web.client.RestClientException

open class KoskiException(
    val suoritusId: KoskiErrorMappingId,
    message: String?,
) : Throwable(message) {
    companion object {
        fun from(
            suoritusId: KoskiErrorMappingId,
            e: RestClientException,
        ): KoskiException {
            val message = e.message ?: e.toString()
            return when (message.take(3)) {
                "400", "409" -> KoskiValidationException(suoritusId, message)
                else -> KoskiTechnicalException(suoritusId, message)
            }
        }
    }
}

class KoskiValidationException(
    suoritusId: KoskiErrorMappingId,
    message: String?,
) : KoskiException(suoritusId, message)

class KoskiTechnicalException(
    suoritusId: KoskiErrorMappingId,
    message: String?,
) : KoskiException(suoritusId, message)
