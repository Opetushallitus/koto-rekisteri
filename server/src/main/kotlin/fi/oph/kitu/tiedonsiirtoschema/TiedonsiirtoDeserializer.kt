package fi.oph.kitu.tiedonsiirtoschema

import com.fasterxml.jackson.databind.JsonMappingException
import fi.oph.kitu.defaultObjectMapper
import fi.oph.kitu.validation.Validation
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

object TiedonsiirtoDeserializer {
    inline fun <reified T> deserializeAndSave(
        json: String,
        save: (data: T) -> TiedonsiirtoResponse,
    ): ResponseEntity<*> =
        try {
            save(defaultObjectMapper.readValue(json, T::class.java))
        } catch (e: JsonMappingException) {
            TiedonsiirtoFailure.badRequest(e.message ?: "JSON mapping failed for unknown reason")
        } catch (e: Validation.ValidationException) {
            TiedonsiirtoFailure(statusCode = HttpStatus.BAD_REQUEST, errors = e.errors.map { it.toString() })
        } catch (e: Throwable) {
            TiedonsiirtoFailure(statusCode = HttpStatus.INTERNAL_SERVER_ERROR, errors = listOf("Internal server error"))
        }.toResponseEntity()
}
