package fi.oph.kitu.koodisto

import fi.oph.kitu.Cache
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity
import kotlin.time.Duration.Companion.hours

@Service
class KoodistoService(
    @Qualifier("koodistopalveluRestClient")
    private val restClient: RestClient,
) {
    fun getKoodiviitteet(koodistoUri: String): List<KoodistopalveluKoodiviite>? = cachedKoodistot.get(koodistoUri)

    private fun fetchKoodisto(koodistoUri: String): List<KoodistopalveluKoodiviite>? =
        restClient
            .get()
            .uri("codeelement/codes/{uri}", mapOf("uri" to koodistoUri))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity<List<KoodistopalveluKoodiviite>>()
            .body

    private val cachedKoodistot =
        Cache<String, List<KoodistopalveluKoodiviite>>(ttl = 1.hours) {
            fetchKoodisto(it)
        }
}
