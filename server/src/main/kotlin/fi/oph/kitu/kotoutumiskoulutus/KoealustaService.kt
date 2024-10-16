package fi.oph.kitu.kotoutumiskoulutus

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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

        logger
            .atInfo()
            .addKeyValue("request.token", koealustaToken)
            .addKeyValue("response.body", response.body)
            .addKeyValue("response.headers", response.headers)
            .log("koealusta response")

        if (response.body == null) {
            return from
        }

        val suorituksetResponse =
            tryParseMoodleResponse<KoealustaSuorituksetResponse>(response.body!!)

        val suoritukset =
            suorituksetResponse.users.flatMap { user ->
                user.completions.map { completion ->
                    val luetunYmmartaminen = completion.results.find { it.name == "luetun ymm\u00e4rt\u00e4minen" }!!
                    val kuullunYmmartaminen = completion.results.find { it.name == "kuullun ymm\u00e4rt\u00e4minen" }!!
                    val puhe = completion.results.find { it.name == "puhe" }!!
                    val kirjoittaminen = completion.results.find { it.name == "kirjoittaminen" }!!
                    KielitestiSuoritus(
                        first_name = user.firstname,
                        last_name = user.lastname,
                        email = user.email,
                        oppija_oid = user.OIDnumber,
                        time_completed = Instant.ofEpochSecond(completion.timecompleted.toLong()),
                        courseid = completion.courseid,
                        coursename = completion.coursename,
                        luetun_ymmartaminen_result_system = luetunYmmartaminen.quiz_result_system,
                        luetun_ymmartaminen_result_teacher = luetunYmmartaminen.quiz_result_teacher,
                        kuullun_ymmartaminen_result_system = kuullunYmmartaminen.quiz_result_system,
                        kuullun_ymmartaminen_result_teacher = kuullunYmmartaminen.quiz_result_teacher,
                        puhe_result_system = puhe.quiz_result_system,
                        puhe_result_teacher = puhe.quiz_result_teacher,
                        kirjoittaminen_result_system = kirjoittaminen.quiz_result_system,
                        kirjottaminen_result_teacher = kirjoittaminen.quiz_result_teacher,
                        total_evaluation_teacher = completion.total_evaluation_teacher,
                        total_evaluation_system = completion.total_evaluation_system,
                    )
                }
            }

        val result = kielitestiSuoritusRepository.saveAll(suoritukset)

        logger.atInfo().addKeyValue("db.saved", result.count()).log("saved suoritukset")

        return suoritukset.maxOfOrNull { it.time_completed } ?: from
    }
}
