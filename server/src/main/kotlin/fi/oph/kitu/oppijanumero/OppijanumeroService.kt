package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.logging.addCondition
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
            require(etunimet.isNotEmpty()) { "etunimet cannot be empty" }
            require(hetu.isNotEmpty()) { "hetu cannot be empty" }
            require(sukunimi.isNotEmpty()) { "sukunimi cannot be empty" }
            require(kutsumanimi.isNotEmpty()) { "kutsumanimi cannot be empty" }

            if (event.addCondition(key = "requestHasOppijanumero", condition = oppijanumero != null)) {
                return@withEvent oppijanumero.toString()
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
                            objectMapper.writeValueAsString(
                                YleistunnisteHaeRequest(
                                    etunimet,
                                    hetu,
                                    kutsumanimi,
                                    sukunimi,
                                ),
                            ),
                        ),
                    ).header("Content-Type", "application/json")

            // no need to log sendRequest, because there are request and response logging inside casAuthenticatedService.
            val httpResponse = casAuthenticatedService.sendRequest(httpRequest)
            val code = httpResponse.statusCode()
            if (code != 200) {
                // This can happen at least in few scenarios:
                //  1. code == 504. Oppijanumero-service goes down while we are having sign-in request -flow.
                //      First requests are passed, but the last request returns 504.
                //  2. code == 5xx. There is an unknown internal error in oppijanumero-service
                //  3. code == 404. Person not found.
                //      - Oppijanumero-service was unable to identify person with SSN and other information.
                //  4. code == 409. Conflict in person information.
                //      - Oppijanumero-service was able to identify person with SSN
                //        but there is a mismatch in etunimet, kutsumanimi and/or sukunimi.
                throw RuntimeException("Unexpected status code '$code' by the endpoint '$endpoint'.")
            }

            val body = objectMapper.readValue(httpResponse.body(), YleistunnisteHaeResponse::class.java)
            if (body.oppijanumero.isNullOrEmpty()) {
                throw RuntimeException("Oppijanumero with hetu '$hetu' and oid '${body.oid}' is not identified.")
            }

            return@withEvent body.oppijanumero
        }
}
