package fi.oph.kitu.kielitesti

import fi.oph.kitu.oppija.Oppija
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class MoodleService(
    private val restClientBuilder: RestClient.Builder,
) {
    data class MoodleOppija(
        val id: Long,
        val fullname: String,
        val extrafields: List<Any>,
    )

    data class MoodleOppijaLista(
        val list: List<MoodleOppija>,
    )

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
                    "/webservice/rest/server.php?wstoken={wstoken}",
                    mapOf(
                        "wstoken" to moodleToken,
                        "wsfunction" to "core_user_search_identity",
                        "moodlewsrestformat" to "json",
                        "query" to "",
                    ),
                ).retrieve()
                .toEntity(MoodleOppijaLista::class.java)
        val oppijat = response.body?.list?.map { Oppija(it.id, it.fullname) }
        return oppijat ?: listOf()
    }
}
