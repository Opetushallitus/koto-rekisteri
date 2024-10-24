package fi.oph.kitu.kotoutumiskoulutus

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.addResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import java.time.Instant

@Service
class KoealustaService(
    private val restClientBuilder: RestClient.Builder,
    private val kielitestiSuoritusRepository: KielitestiSuoritusRepository,
    private val jacksonObjectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${kitu.kotoutumiskoulutus.koealusta.wstoken}")
    lateinit var koealustaToken: String

    @Value("\${kitu.kotoutumiskoulutus.koealusta.baseurl}")
    lateinit var koealustaBaseUrl: String

    private val restClient by lazy { restClientBuilder.baseUrl(koealustaBaseUrl).build() }

    private inline fun <reified T> tryParseMoodleResponse(json: String): T {
        try {
            return jacksonObjectMapper.enable(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION).readValue<T>(json)
        } catch (e: Exception) {
            val moodleError = jacksonObjectMapper.readValue<MoodleErrorMessage>(json)
            throw MoodleException(moodleError)
        }
    }

    fun importSuoritukset(from: Instant): Instant {
        val event =
            logger
                .atInfo()
                .add(
                    "operation" to "koealusta.import.suoritukset",
                    "from" to from,
                )

        try {
            val response =
                restClient
                    .get()
                    .uri(
                        "/webservice/rest/server.php?wstoken={token}&wsfunction={function}&moodlewsrestformat=json&from={from}",
                        mapOf<String?, Any>(
                            "token" to koealustaToken,
                            "function" to "local_completion_export_get_completions",
                            "from" to from.epochSecond,
                        ),
                    ).accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .toEntity<String>()

            event
                .add("request.token" to koealustaToken)
                .addResponse(response)

            if (response.body == null) {
                return from
            }

            val suorituksetResponse =
                tryParseMoodleResponse<KoealustaSuorituksetResponse>(response.body!!)

            val suoritukset =
                suorituksetResponse.users.flatMap { user ->
                    user.completions.map { completion ->
                        val luetunYmmartaminen =
                            completion.results.find {
                                it.name == "luetun ymm\u00e4rt\u00e4minen"
                            }!!
                        val kuullunYmmartaminen =
                            completion.results.find {
                                it.name == "kuullun ymm\u00e4rt\u00e4minen"
                            }!!
                        val puhe = completion.results.find { it.name == "puhe" }!!
                        val kirjoittaminen = completion.results.find { it.name == "kirjoittaminen" }!!
                        KielitestiSuoritus(
                            firstName = user.firstname,
                            lastName = user.lastname,
                            email = user.email,
                            oppijaOid = user.OIDnumber,
                            timeCompleted = Instant.ofEpochSecond(completion.timecompleted),
                            courseid = completion.courseid,
                            coursename = completion.coursename,
                            luetunYmmartaminenResultSystem = luetunYmmartaminen.quiz_result_system,
                            luetunYmmartaminenResultTeacher = luetunYmmartaminen.quiz_result_teacher,
                            kuullunYmmartaminenResultSystem = kuullunYmmartaminen.quiz_result_system,
                            kuullunYmmartaminenResultTeacher = kuullunYmmartaminen.quiz_result_teacher,
                            puheResultSystem = puhe.quiz_result_system,
                            puheResultTeacher = puhe.quiz_result_teacher,
                            kirjoittaminenResultSystem = kirjoittaminen.quiz_result_system,
                            kirjottaminenResultTeacher = kirjoittaminen.quiz_result_teacher,
                            totalEvaluationTeacher = completion.total_evaluation_teacher,
                            totalEvaluationSystem = completion.total_evaluation_system,
                        )
                    }
                }

            val result = kielitestiSuoritusRepository.saveAll(suoritukset)

            event.add("db.saved" to result.count())

            return suoritukset.maxOfOrNull { it.timeCompleted } ?: from
        } catch (e: Exception) {
            event.setCause(e)
            throw e
        } finally {
            event.log()
        }
    }
}
