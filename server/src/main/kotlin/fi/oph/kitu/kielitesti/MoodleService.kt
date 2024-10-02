package fi.oph.kitu.kielitesti

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import fi.oph.kitu.oppija.Oppija
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

data class MoodleUser(
    val id: Long,
    val fullname: String,
    val extrafields: List<Any>,
)

data class MoodleUserList(
    val list: List<MoodleUser>,
    val maxusersperpage: Long?,
    val overflow: Boolean?,
) {
    companion object {
        private val mapper = ObjectMapper().registerKotlinModule()

        fun tryParse(json: String): List<Oppija> {
            try {
                val body = mapper.readValue<MoodleUserList>(json)
                return body.list.map { Oppija(it.id, it.fullname) } ?: listOf()
            } catch (e: Exception) {
                val moodleError = mapper.readValue<MoodleErrorMessage>(json)
                throw MoodleException(moodleError)
            }
        }
    }
}

class MoodleException(
    val moodleErrorMessage: MoodleErrorMessage,
) : Exception(moodleErrorMessage.message)

data class MoodleErrorMessage(
    val exception: String,
    val errorcode: String,
    val message: String,
    val debuginfo: String?,
)

@Service
class MoodleService(
    private val restClientBuilder: RestClient.Builder,
) {
    @Value("\${kitu.kielitesti.wstoken}")
    lateinit var moodleToken: String

    @Value("\${kitu.kielitesti.baseurl}")
    lateinit var kielitestiBaseurl: String

    private val restClient by lazy { restClientBuilder.baseUrl(kielitestiBaseurl).build() }

    fun getUsers(): List<Oppija> {
        val response =
            restClient
                .get()
                .uri(
                    "/webservice/rest/server.php?wstoken={token}&wsfunction={function}&moodlewsrestformat=json&query=",
                    mapOf(
                        "token" to moodleToken,
                        "function" to "core_user_search_identity",
                    ),
                ).accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String::class.java)

        if (response.isNullOrEmpty()) return listOf()
        return MoodleUserList.tryParse(response)
    }
}
