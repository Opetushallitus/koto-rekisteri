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
    fun getOppijanumero(oppija: Oppija): String
}

class Oppija(
    val etunimet: String,
    val hetu: String,
    val kutsumanimi: String,
    val sukunimi: String,
    val oppijanumero: String? = null,
    val henkilo_oid: String? = null,
) {
    fun withYleistunnisteHaeResponse(response: YleistunnisteHaeResponse) =
        Oppija(etunimet, hetu, kutsumanimi, sukunimi, response.oppijanumero, henkilo_oid = response.oid)

    fun toYleistunnisteHaeRequest() =
        YleistunnisteHaeRequest(
            etunimet,
            hetu,
            kutsumanimi,
            sukunimi,
        )
}

class OppijanumeroException(
    val oppija: Oppija,
    /**
     * The status code code that was returned by oppijanumero-service. Possible explanations:
     *  1. status code == 404. Person not found.
     *      - Oppijanumero-service was unable to identify person with SSN and other information.
     *  2. status code == 409. Conflict in person information.
     *      - Oppijanumero-service was able to identify person with SSN
     *      but there is a mismatch in etunimet, kutsumanimi and/or sukunimi.
     *  3. status code == 504. Oppijanumero-service goes down while we are having sign-in request -flow.
     *       First requests are passed, but the last request returns 504.
     *  4. status code == 5xx. There is an unknown internal error in oppijanumero-service
     * */
    val statusCode: Int? = null,
    message: String,
) : RuntimeException(message)

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
            val httpResponse = casAuthenticatedService.sendRequest(httpRequest)
            val code = httpResponse.statusCode()

            val body = objectMapper.readValue(httpResponse.body(), YleistunnisteHaeResponse::class.java)

            if (code != 200) {
                throw OppijanumeroException(
                    oppija.withYleistunnisteHaeResponse(body),
                    statusCode = code,
                    "Unexpected status code '$code' by the endpoint '$endpoint'.",
                )
            }

            if (body.oppijanumero.isNullOrEmpty()) {
                throw OppijanumeroException(
                    oppija.withYleistunnisteHaeResponse(body),
                    statusCode = code,
                    "Oppija is not identified",
                )
            }

            return@withEvent body.oppijanumero
        }
}
