package fi.oph.kitu.kielitesti

import fi.oph.kitu.oppija.Oppija
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class MoodleService(
    private val restTemplate: RestTemplate,
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

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun getUsers(): List<Oppija> {
        val response: ResponseEntity<MoodleOppijaLista> =
            restTemplate.getForEntity(
                "$kielitestiBaseurl/webservice/rest/server.php?wstoken=$moodleToken&wsfunction=core_user_search_identity&moodlewsrestformat=json&query=",
                MoodleOppijaLista::class.java,
            )
        val oppijat = response.body?.list?.map { Oppija(it.id, it.fullname) }
        return oppijat ?: listOf()
    }
}
