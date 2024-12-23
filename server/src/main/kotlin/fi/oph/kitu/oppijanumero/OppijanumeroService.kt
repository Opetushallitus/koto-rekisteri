package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.logging.addCondition
import fi.oph.kitu.logging.withEvent
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpRequest

interface OppijanumeroService {
    fun getOppijanumero(oppija: Oppija): String
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

    override fun getOppijanumero(oppija: Oppija): String =
        logger.atInfo().withEvent("getOppijanumero") { event ->
            require(oppija.etunimet.isNotEmpty()) { "etunimet cannot be empty" }
            require(oppija.hetu.isNotEmpty()) { "hetu cannot be empty" }
            require(oppija.sukunimi.isNotEmpty()) { "sukunimi cannot be empty" }
            require(oppija.kutsumanimi.isNotEmpty()) { "kutsumanimi cannot be empty" }

            if (event.addCondition(key = "requestHasOppijanumero", condition = oppija.oppijanumero != null)) {
                return@withEvent oppija.oppijanumero.toString()
            }

            if (event.addCondition(key = "useMockData", condition = useMockData)) {
                return@withEvent "1.2.246.562.24.33342764709"
            }

            val endpoint = "$serviceUrl/yleistunniste/hae"
            val httpRequest =
                HttpRequest
                    .newBuilder(URI.create(endpoint))
                    .POST(
                        HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(oppija.toYleistunnisteHaeRequest()),
                        ),
                    ).header("Content-Type", "application/json")

            // no need to log sendRequest, because there are request and response logging inside casAuthenticatedService.
            val result =
                casAuthenticatedService
                    .sendRequest(httpRequest)
                    .getOrLogAndThrowCasException(event)

            val body = objectMapper.readValue(result, YleistunnisteHaeResponse::class.java)

            if (body.oppijanumero.isNullOrEmpty()) {
                throw OppijanumeroException(
                    oppija.withYleistunnisteHaeResponse(body),
                    "Oppija is not identified",
                )
            }

            return@withEvent body.oppijanumero
        }
}
