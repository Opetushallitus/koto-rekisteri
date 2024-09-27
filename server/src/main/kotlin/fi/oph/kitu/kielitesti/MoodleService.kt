package fi.oph.kitu.kielitesti

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class MoodleService(
    private val restClientBuilder: RestClient.Builder,
) {
    @Value("\${kitu.kielitesti.wstoken}")
    lateinit var moodleToken: String

    @Value("\${kitu.kielitesti.baseurl}")
    lateinit var kielitestiBaseurl: String

    private val restClient by lazy { restClientBuilder.baseUrl(kielitestiBaseurl).build() }

    fun getUsers(): GetUsersResponse =
        callMoodle<GetUsersResponse>("core_user_search_identity")
            ?: GetUsersResponse.Success(emptyList())

    private inline fun <reified T> callMoodle(function: String) =
        restClient
            .get()
            .uri(
                "/webservice/rest/server.php?wstoken={token}&wsfunction={function}&moodlewsrestformat={format}&query=",
                mapOf(
                    "token" to moodleToken,
                    "function" to function,
                    "format" to "json",
                ),
            ).accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(T::class.java)
}
