package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.logging.addCondition
import fi.oph.kitu.logging.withEvent
import fi.oph.kitu.logging.withTryCatch
import org.slf4j.Logger
import org.slf4j.spi.LoggingEventBuilder
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

interface OppijanumeroService {
    fun getOppijanumero(oppija: Oppija): String
}

@Service
class OppijanumeroServiceImpl(
    private val casAuthenticatedService: CasAuthenticatedService,
    val objectMapper: ObjectMapper,
    @Qualifier("oppijanumeroServiceLogger")
    private val logger: Logger,
) : OppijanumeroService {
    @Value("\${kitu.oppijanumero.service.url}")
    lateinit var serviceUrl: String

    @Value("\${kitu.oppijanumero.service.use-mock-data}")
    var useMockData: Boolean = false

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
            val stringResponse =
                casAuthenticatedService
                    .sendRequest(httpRequest)
                    .getOrLogAndThrowCasException(event)

            if (stringResponse.statusCode() == 404) {
                throw OppijanumeroException.OppijaNotFoundException(oppija)
            } else if (stringResponse.statusCode() != 200) {
                throw OppijanumeroException(
                    oppija,
                    "Oppijanumero-service returned unexpected status code ${stringResponse.statusCode()}",
                )
            }

            val body = tryConvertToOppijanumeroResponse<YleistunnisteHaeResponse>(oppija, stringResponse, event)

            if (body.oppijanumero.isNullOrEmpty()) {
                throw OppijanumeroException.OppijaNotIdentifiedException(
                    oppija.withYleistunnisteHaeResponse(body),
                )
            }

            return@withEvent body.oppijanumero
        }

    /**
     * Tries to convert HttpResponse<String> into the given T.
     * If the conversion fails, it checks whether the response was OppijanumeroServiceError.
     * In that case OppijanumeroException will be thrown.
     * Otherwise the underlying exception will be thrown
     */
    final inline fun <reified T> tryConvertToOppijanumeroResponse(
        oppija: Oppija,
        response: HttpResponse<String>,
        event: LoggingEventBuilder,
    ): T =
        event.withTryCatch(
            key = "oppijanumero.parse-response.success",
            action = {
                val value = objectMapper.readValue(response.body(), T::class.java)
                return@withTryCatch value
            },
            onFailure = { exception ->
                val error = tryParseOppijanumeroError(response.body(), exception, event)

                throw OppijanumeroException(
                    oppija,
                    "Error from oppijanumero-service: ${error.error}",
                    error,
                )
            },
        )

    fun tryParseOppijanumeroError(
        json: String,
        originalException: Throwable,
        event: LoggingEventBuilder,
    ): OppijanumeroServiceError =
        event.withTryCatch(
            key = "oppijanumero.parse-oppijanumero-error.success",
            action = {
                // assume that exception was caused, because response was OppijanumeroServiceError
                objectMapper.readValue(json, OppijanumeroServiceError::class.java)
            },
            onFailure = {
                // If the assumption was wrong, then throw the original error,
                // in order to see what is wrong.
                throw originalException
            },
        )
}
