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
            e.toTiedonsiirtoFailure()
        } catch (e: Validation.ValidationException) {
            TiedonsiirtoFailure(statusCode = HttpStatus.BAD_REQUEST, errors = e.errors.map { it.toString() })
        } catch (e: Throwable) {
            TiedonsiirtoFailure(statusCode = HttpStatus.INTERNAL_SERVER_ERROR, errors = listOf("Internal server error"))
        }.toResponseEntity()
}

fun JsonMappingException.toTiedonsiirtoFailure(): TiedonsiirtoFailure {
    val regex = Regex(".*\\[\"(.*)\"]")
    val simplePath =
        pathReference
            .split("->")
            .mapNotNull { t ->
                regex.find(t)?.let { it.groupValues[1] }
            }.joinToString(".")

    val simpleMessage =
        localizedMessage
            .split("\n")
            .first()
            .replace("fi.oph.kitu.tiedonsiirtoschema.", "")
            .replace("fi.oph.kitu.", "")

    return TiedonsiirtoFailure.badRequest("$simplePath: $simpleMessage")
}
