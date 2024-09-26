package fi.oph.kitu.kielitesti

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import fi.oph.kitu.oppija.Oppija
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

sealed class ParsedUserList {
    data class Success(
        val users: List<Oppija>,
    ) : ParsedUserList()

    data class Failure(
        val error: MoodleError,
    ) : ParsedUserList()

    companion object {
        private val mapper = ObjectMapper().registerKotlinModule()
        private val logger: Logger = LoggerFactory.getLogger(ParsedUserList::class.java)

        fun tryParse(json: String): ParsedUserList {
            try {
                val body = mapper.readValue<MoodleUserList>(json)
                return Success(body.list.map { Oppija(it.id, it.fullname) } ?: listOf())
            } catch (e: Exception) {
                val moodleError = mapper.readValue<MoodleError>(json)
                logger.error(moodleError.toString())
                return Failure(moodleError)
            }
        }
    }
}

data class MoodleUser(
    val id: Long,
    val fullname: String,
    val extrafields: List<Any>,
)

data class MoodleUserList(
    val list: List<MoodleUser>,
    val maxusersperpage: Long?,
    val overflow: Boolean?,
)

data class MoodleError(
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

    fun getUsers(): ParsedUserList {
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

        if (response.isNullOrEmpty()) return ParsedUserList.Success(listOf())
        return ParsedUserList.tryParse(response)
    }
}
