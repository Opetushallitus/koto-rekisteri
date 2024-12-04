package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
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
    private val openTelemetry: OpenTelemetry,
) : OppijanumeroService {
    @Value("\${kitu.oppijanumero.service.url}")
    private lateinit var serviceUrl: String

    @Value("\${kitu.oppijanumero.service.use-mock-data}")
    private var useMockData: Boolean = false

    private val tracer = openTelemetry.getTracer("oppijanumero-service")

    fun Span.setAttributeWithCondition(
        key: String,
        value: Boolean,
    ): Boolean {
        setAttribute(key, value)
        return value
    }

    override fun getOppijanumero(
        etunimet: String,
        hetu: String,
        kutsumanimi: String,
        sukunimi: String,
        oppijanumero: String?,
    ): String {
        val span = tracer.spanBuilder("getOppijanumero").startSpan()

        require(etunimet.isNotEmpty()) { "etunimet cannot be empty" }
        require(hetu.isNotEmpty()) { "hetu cannot be empty" }
        require(sukunimi.isNotEmpty()) { "sukunimi cannot be empty" }
        require(kutsumanimi.isNotEmpty()) { "kutsumanimi cannot be empty" }

        span.setAttribute("requestHasOppijanumero", oppijanumero != null)

        if (span.setAttributeWithCondition("requestHasOppijanumero", oppijanumero != null)) {
            return oppijanumero.toString()
        }

        if (span.setAttributeWithCondition("useMockData", useMockData)) {
            return "1.2.246.562.24.33342764709"
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
            throw RuntimeException("Unexpected status code '$code' by the endpoint '$endpoint'.")
        }

        val body = objectMapper.readValue(httpResponse.body(), YleistunnisteHaeResponse::class.java)
        if (body.oppijanumero.isNullOrEmpty()) {
            throw RuntimeException("Oppijanumero with hetu '$hetu' and oid '${body.oid}' is not identified.")
        }

        return body.oppijanumero
    }
}
