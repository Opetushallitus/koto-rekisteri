package fi.oph.kitu.koodisto

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.toEntity

@Service
class KoodistoService(
    @Qualifier("koodistopalveluRestClient")
    private val restClient: RestClient,
) {
    private val koodistot: MutableMap<String, List<KoodistopalveluKoodiviite>> = mutableMapOf()

    fun getKoodiviitteet(koodistoUri: String): List<KoodistopalveluKoodiviite>? {
        if (koodistot.containsKey(koodistoUri)) return koodistot[koodistoUri]
        return restClient
            .get()
            .uri("codeelement/codes/{uri}", mapOf("uri" to koodistoUri))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity<List<KoodistopalveluKoodiviite>>()
            .body
            ?.also { koodistot[koodistoUri] = it }
    }
}
