package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.TypedResult
import fi.oph.kitu.logging.add
import fi.oph.kitu.logging.addCondition
import fi.oph.kitu.logging.withEventAndPerformanceCheck
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpRequest
import java.net.http.HttpResponse

interface OppijanumeroService {
    fun getOppijanumero(oppija: Oppija): TypedResult<String, OppijanumeroException>
}

@Service
class OppijanumeroServiceImpl(
    private val casAuthenticatedService: CasAuthenticatedService,
    val objectMapper: ObjectMapper,
) : OppijanumeroService {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Value("\${kitu.oppijanumero.service.url}")
    lateinit var serviceUrl: String

    override fun getOppijanumero(oppija: Oppija): TypedResult<String, OppijanumeroException> =
        logger
            .atInfo()
            .withEventAndPerformanceCheck { event ->
                require(oppija.etunimet.isNotEmpty()) { "etunimet cannot be empty" }
                require(oppija.hetu.isNotEmpty()) { "hetu cannot be empty" }
                require(oppija.sukunimi.isNotEmpty()) { "sukunimi cannot be empty" }
                require(oppija.kutsumanimi.isNotEmpty()) { "kutsumanimi cannot be empty" }

                if (event.addCondition(key = "request.hasOppijanumero", condition = oppija.oppijanumero != null)) {
                    return@withEventAndPerformanceCheck oppija.oppijanumero.toString()
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

                val body = tryConvertToOppijanumeroResponse<YleistunnisteHaeResponse>(oppija, stringResponse)
                event.add(
                    "response.hasOppijanumero" to body.oppijanumero.isNullOrEmpty(),
                    "response.hasOid" to body.oid.isEmpty(),
                    "response.areOppijanumeroAndOidSame" to (body.oppijanumero == body.oid),
                )

                if (body.oppijanumero.isNullOrEmpty()) {
                    throw OppijanumeroException.OppijaNotIdentifiedException(
                        oppija.withYleistunnisteHaeResponse(body),
                    )
                }

                return@withEventAndPerformanceCheck body.oppijanumero
            }.apply {
                addDefaults("getOppijanumero")
            }.result
            .fold(
                onSuccess = { TypedResult.Success(it) },
                onFailure = {
                    when (it) {
                        is OppijanumeroException -> TypedResult.Failure(it)
                        else -> throw it
                    }
                },
            )

    /**
     * Tries to convert HttpResponse<String> into the given T.
     * If the conversion fails, it checks whether the response was OppijanumeroServiceError.
     * In that case OppijanumeroException will be thrown.
     * Otherwise, the underlying exception will be thrown
     */
    final inline fun <reified T> tryConvertToOppijanumeroResponse(
        oppija: Oppija,
        response: HttpResponse<String>,
    ): T =
        runCatching {
            objectMapper.readValue(response.body(), T::class.java)
        }.onFailure { exception ->
            val error =
                runCatching {
                    objectMapper.readValue(response.body(), OppijanumeroServiceError::class.java)
                }.onFailure { _ -> throw exception }
                    .getOrThrow()

            throw OppijanumeroException(
                oppija,
                "Error from oppijanumero-service: ${error.error}",
                error,
            )
        }.getOrThrow()
}
