package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.withEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import java.net.URI
import java.net.http.HttpRequest

interface OppijanumeroService {
    fun getOppijanumero(
        etunimet: String,
        hetu: String,
        kutsumanimi: String,
        sukunimi: String,
        oppijanumero: String? = "",
    ): String
}

@Service
class OppijanumeroServiceImpl(
    private val casAuthenticatedService: CasAuthenticatedService,
    private val objectMapper: ObjectMapper,
) : OppijanumeroService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${kitu.oppijanumero.service.url}")
    private lateinit var serviceUrl: String

    @Value("\${kitu.oppijanumero.service.use-mock-data}")
    private var useMockData: Boolean = false

    override fun getOppijanumero(
        etunimet: String,
        hetu: String,
        kutsumanimi: String,
        sukunimi: String,
        oppijanumero: String?,
    ): String =
        logger.atInfo().withEvent("getOppijanumero") { event ->
            require(etunimet.isEmpty()) { "etunimet cannot be empty" }
            require(hetu.isEmpty()) { "hetu cannot be empty" }
            require(sukunimi.isEmpty()) { "sukunimi cannot be empty" }
            require(kutsumanimi.isEmpty()) { "kutsumanimi cannot be empty" }

            if (oppijanumero != null) {
                event.add("requestHasOppijanumero" to true)
                return@withEvent oppijanumero
            }

            event.add("requestHasOppijanumero" to false)

            if (useMockData) {
                event.add("useMockData" to true)
                return@withEvent "1.2.246.562.24.33342764709"
            }

            event.add("useMockData" to false)

            val endpoint = "$serviceUrl/yleistunniste/hae"
            val httpRequest =
                HttpRequest
                    .newBuilder(URI.create(endpoint))
                    .POST(
                        HttpRequest.BodyPublishers.ofString(
                            """{
                                "etunimet": "$etunimet",
                                "hetu": "$hetu",
                                "kutsumanimi": "$kutsumanimi",
                                "sukunimi": "$sukunimi"
                            }""".trim(),
                        ),
                    ).header("Content-Type", "application/json")

            // no need to log sendRequest, because there are request and reponse logging inside casAuthenticatedService.
            val httpResponse = casAuthenticatedService.sendRequest(httpRequest)
            val code = httpResponse.statusCode()
            if (code != 200) {
                throw RuntimeException("Unexpected status code '$code' by the endpoint '$endpoint'.")
            }

            val body = objectMapper.readValue(httpResponse.body(), YleistunnisteHaeResponse::class.java)
            if (body.oppijanumero.isNullOrEmpty()) {
                throw RuntimeException("Oppijanumero with hetu '$hetu' and oid '${body.oid}' is not identified.")
            }

            return@withEvent body.oppijanumero
        }
}
