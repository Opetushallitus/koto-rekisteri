package fi.oph.kitu.kotoutumiskoulutus

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import fi.oph.kitu.PeerService
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.addResponse
import fi.oph.kitu.logging.withEvent
import fi.oph.kitu.oppijanumero.OppijanumeroService
import fi.oph.kitu.oppijanumero.YleistunnisteHaeRequest
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
    private val oppijanumeroService: OppijanumeroService,
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
            throw tryParseMoodleError(json, e)
        }
    }

    private fun tryParseMoodleError(
        json: String,
        originalException: Exception,
    ): MoodleException {
        try {
            return MoodleException(jacksonObjectMapper.readValue<MoodleErrorMessage>(json))
        } catch (e: Exception) {
            throw RuntimeException(
                "Could not parse Moodle error message: ${e.message} while handling parsing error",
                originalException,
            )
        }
    }

    fun getOid(user: KoealustaSuorituksetResponse.User): String {
        val oid =
            if (user.OID.isEmpty()) {
                val (_, oppija) =
                    oppijanumeroService.yleistunnisteHae(
                        YleistunnisteHaeRequest(
                            etunimet = user.firstname,
                            sukunimi = user.lastname,
                            hetu = user.SSN,
                            kutsumanimi = user.preferredname,
                        ),
                    )
                oppija.oid
            } else {
                user.OID
            }

        if (oid.isEmpty()) {
            throw RuntimeException("oid is empty")
        }

        return oid
    }

    fun importSuoritukset(from: Instant): Instant =
        logger.atInfo().withEvent("koealusta.importSuoritukset") { event ->
            event.add("from" to from)

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
                .addResponse(response, PeerService.Koealusta)

            if (response.body == null) {
                return@withEvent from
            }

            val suorituksetResponse =
                tryParseMoodleResponse<KoealustaSuorituksetResponse>(response.body!!)

            val suoritukset =
                suorituksetResponse.users.flatMap { user ->
                    val oid = getOid(user)
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
                            oppijaOid = oid,
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

            return@withEvent suoritukset.maxOfOrNull { it.timeCompleted } ?: from
        }
}
