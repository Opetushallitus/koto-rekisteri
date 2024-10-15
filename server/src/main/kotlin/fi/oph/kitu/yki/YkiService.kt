package fi.oph.kitu.yki

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import fi.oph.kitu.csvBody
import fi.oph.kitu.generated.model.YkiSuoritus
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class YkiService(
    @Qualifier("ykiRestClient")
    private val ykiRestClient: RestClient,
    private val repository: YkiRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun importYkiSuoritukset() {
        val suoritukset =
            ykiRestClient
                .get()
                .uri("/yki")
                .retrieve()
                .csvBody<Suoritus>()

        if (suoritukset == null) {
            logger.info("No YKI suoritukset found")
            return
        }

        val repData: List<YkiSuoritus> = throw NotImplementedError()
        repository.insertSuoritukset(repData)
        logger.info("YKI Suoritukset was added to repository")
    }

    @JsonPropertyOrder(
        "suorittajanOppijanumero",
        "sukunimi",
        "etunimet",
        "tutkintopaiva",
        "tutkintokieli",
        "tutkintotaso",
        "jarjestajanTunnusOid",
        "jarjestajanNimi",
        "tekstinYmmartaminen",
        "kirjoittaminen",
        "rakenteetJaSanasto",
        "puheenYmmartaminen",
        "puhuminen",
        "yleisarvosana",
    )
    data class Suoritus(
        val suorittajanOppijanumero: String,
        val sukunimi: String,
        val etunimet: String,
        val tutkintopaiva: String, // ISO-8601-muodossa
        val tutkintokieli: String, // ISO 649-2 alpha-3 -muodossa
        val tutkintotaso: String, // ("PT"=perustaso, "KT"=keskitaso, "YT"=ylin taso)
        val jarjestajanTunnusOid: String,
        val jarjestajanNimi: String,
        val tekstinYmmartaminen: Number,
        val kirjoittaminen: Number,
        val rakenteetJaSanasto: Number,
        val puheenYmmartaminen: Number,
        val puhuminen: Number,
        val yleisarvosana: Number,
    )
}
