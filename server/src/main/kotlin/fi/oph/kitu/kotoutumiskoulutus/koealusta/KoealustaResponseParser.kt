package fi.oph.kitu.kotoutumiskoulutus.koealusta

import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue

@Service
class KoealustaResponseParser(
    private val jacksonObjectMapper: ObjectMapper,
) {
    fun parse(json: String): KoealustaSuorituksetResponse =
        try {
            jacksonObjectMapper.readValue<KoealustaSuorituksetResponse>(json)
        } catch (e: Throwable) {
            throw parseMoodleError(json, e)
        }

    private fun parseMoodleError(
        json: String,
        originalException: Throwable,
    ): MoodleException =
        try {
            MoodleException(jacksonObjectMapper.readValue<MoodleErrorMessage>(json))
        } catch (e: Throwable) {
            throw RuntimeException(
                "Could not parse Moodle error message: ${e.message} while handling parsing error",
                originalException,
            )
        }
}
