package fi.oph.kitu.yki

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.time.LocalDate

@Service
class YkiService(
    @Qualifier("ykiRestClient")
    private val ykiRestClient: RestClient,
    private val repository: YkiRepository,
) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun importYkiSuoritukset(lastSeen: LocalDate? = null) {
        val spec =
            ykiRestClient
                .get()
                .uri("suoritukset")
                .retrieve()

        val csvMapper = CsvMapper()
        val schema =
            csvMapper
                .typedSchemaFor(Suoritus::class.java)
                .withColumnSeparator(',')
                .withUseHeader(false)
                .withQuoteChar('"')

        val bodyAsString = spec.body(String::class.java)
        val suoritus =
            csvMapper
                .readerFor(Suoritus::class.java)
                .with(schema)
                .readValue<Suoritus>(bodyAsString)

        println(suoritus)
        throw NotImplementedError()
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
        @JsonProperty("suorittajanOppijanumero")
        val suorittajanOppijanumero: String,
        @JsonProperty("sukunimi")
        val sukunimi: String,
        @JsonProperty("etunimet")
        val etunimet: String,
        @JsonProperty("tutkintopaiva")
        val tutkintopaiva: String, // ISO-8601-muodossa
        @JsonProperty("tutkintokieli")
        val tutkintokieli: String, // ISO 649-2 alpha-3 -muodossa
        @JsonProperty("tutkintotaso")
        val tutkintotaso: String, // ("PT"=perustaso, "KT"=keskitaso, "YT"=ylin taso)
        @JsonProperty("jarjestajanTunnusOid")
        val jarjestajanTunnusOid: String,
        @JsonProperty("jarjestajanNimi")
        val jarjestajanNimi: String,
        @JsonProperty("tekstinYmmartaminen")
        val tekstinYmmartaminen: Number,
        @JsonProperty("kirjoittaminen")
        val kirjoittaminen: Number,
        @JsonProperty("rakenteetJaSanasto")
        val rakenteetJaSanasto: Number,
        @JsonProperty("puheenYmmartaminen")
        val puheenYmmartaminen: Number,
        @JsonProperty("puhuminen")
        val puhuminen: Number,
        @JsonProperty("yleisarvosana")
        val yleisarvosana: Number,
    )
}
