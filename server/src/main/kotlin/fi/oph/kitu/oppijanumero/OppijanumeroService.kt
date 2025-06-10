package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.logging.use
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.net.URI
import java.net.http.HttpRequest
import java.time.LocalDate
import java.time.OffsetDateTime

interface OppijanumeroService {
    fun getOppijanumero(oppija: Oppija): TypedResult<Oid, OppijanumeroException>

    fun getHenkilo(oid: Oid): TypedResult<OppijanumerorekisteriHenkilo, OppijanumeroException>
}

@Service
class OppijanumeroServiceImpl(
    casAuthenticatedService: CasAuthenticatedService,
    val objectMapper: ObjectMapper,
    val tracer: Tracer,
) : OppijanumeroService {
    @Value("\${kitu.oppijanumero.service.url}")
    lateinit var serviceUrl: String

    private val client by lazy {
        OppijanumerorekisteriClient(serviceUrl, casAuthenticatedService, objectMapper)
    }

    override fun getOppijanumero(oppija: Oppija): TypedResult<Oid, OppijanumeroException> =
        withSpan("OppijanumeroServiceImpl.getOppijanumero") { span ->
            require(oppija.etunimet.isNotEmpty()) { "etunimet cannot be empty" }
            require(oppija.hetu.isNotEmpty()) { "hetu cannot be empty" }
            require(oppija.sukunimi.isNotEmpty()) { "sukunimi cannot be empty" }
            require(oppija.kutsumanimi.isNotEmpty()) { "kutsumanimi cannot be empty" }

            val requestBody =
                YleistunnisteHaeRequest(
                    oppija.etunimet,
                    oppija.hetu,
                    oppija.kutsumanimi,
                    oppija.sukunimi,
                )

            client
                .post<YleistunnisteHaeResponse, YleistunnisteHaeRequest>("yleistunniste/hae", requestBody)
                .flatMap { body ->
                    span.setAttribute("response.hasOppijanumero", body.oppijanumero.isNullOrEmpty())
                    span.setAttribute("response.hasOid", body.oid.isEmpty())
                    span.setAttribute("response.areOppijanumeroAndOidSame", (body.oppijanumero == body.oid))

                    if (body.oppijanumero.isNullOrEmpty()) {
                        TypedResult.Failure(OppijanumeroException.OppijaNotIdentifiedException(requestBody))
                    } else {
                        Oid
                            .parseTyped(body.oppijanumero)
                            .mapFailure {
                                OppijanumeroException.MalformedOppijanumero(
                                    requestBody,
                                    body.oppijanumero,
                                )
                            }
                    }
                }
        }

    override fun getHenkilo(oid: Oid): TypedResult<OppijanumerorekisteriHenkilo, OppijanumeroException> =
        withSpan("OppijanumeroServiceImpl.getHenkilo") {
            client.get<OppijanumerorekisteriHenkilo>("henkilo/$oid")
        }

    final inline fun <reified T> withSpan(
        s: String,
        block: (Span) -> T,
    ): T =
        tracer
            .spanBuilder(s)
            .startSpan()
            .use { return@use block(it) }
}

class OppijanumerorekisteriClient(
    val serviceUrl: String,
    val casAuthenticatedService: CasAuthenticatedService,
    val objectMapper: ObjectMapper,
) {
    inline fun <reified T> get(endpoint: String) =
        execute<T, EmptyRequest>(endpoint) {
            EmptyRequest() to it.GET()
        }

    inline fun <reified T, R : OppijanumerorekisteriRequest> post(
        endpoint: String,
        body: R,
    ) = execute<T, R>(endpoint) {
        body to
            it.POST(
                HttpRequest.BodyPublishers.ofString(
                    objectMapper.writeValueAsString(body),
                ),
            )
    }

    inline fun <reified T, R : OppijanumerorekisteriRequest> execute(
        endpoint: String,
        init: (HttpRequest.Builder) -> Pair<R, HttpRequest.Builder>,
    ): TypedResult<T, OppijanumeroException> {
        val url = "$serviceUrl/$endpoint"

        val (requestBody, httpRequest) =
            HttpRequest
                .newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .let(init)

        // no need to log sendRequest, because there are request and response logging inside casAuthenticatedService.
        val rawResult =
            casAuthenticatedService.post(
                url,
                requestBody,
                MediaType.APPLICATION_JSON,
                String::class.java,
            )
        if (rawResult !is TypedResult.Success) {
            // CAS errors are not caused by the oppija data, and thus
            // should be handling outside default error handling flow.
            throw (rawResult as TypedResult.Failure).error
        }

        // At this point, CAS-authentication is done succesfully,
        // but we still need to check endpoint specific statuses
        val rawResponse = rawResult.value
        if (rawResponse.statusCode == HttpStatus.NOT_FOUND) {
            return TypedResult.Failure(
                OppijanumeroException.OppijaNotFoundException(requestBody),
            )
        } else if (rawResponse.statusCode.is4xxClientError) {
            return TypedResult.Failure(
                OppijanumeroException.BadRequest(requestBody, rawResponse),
            )
        } else if (!rawResponse.statusCode.is2xxSuccessful) {
            throw OppijanumeroException.UnexpectedError(requestBody, rawResponse)
        }

        return deserializeResponse<T>(requestBody, rawResult.value)
    }

    /**
     * Tries to convert `HttpResponse<String>` into the given `T`.
     * If the conversion fails, it checks whether the response was OppijanumeroServiceError.
     * In that case [OppijanumeroException.BadResponse] will be thrown.
     * Otherwise, the underlying exception will be thrown
     */
    final inline fun <reified T> deserializeResponse(
        request: OppijanumerorekisteriRequest,
        response: ResponseEntity<String>,
    ): TypedResult<T, OppijanumeroException> =
        TypedResult
            .runCatching {
                objectMapper.readValue(response.body, T::class.java)
            }.mapFailure { decodeError ->
                TypedResult
                    .runCatching {
                        objectMapper.readValue(
                            response.body,
                            OppijanumeroServiceError::class.java,
                        )
                    }.fold(
                        onSuccess = { onrError ->
                            OppijanumeroException.BadResponse(
                                request = request,
                                response = response,
                                oppijanumeroServiceError = onrError,
                                cause = decodeError,
                            )
                        },
                        onFailure = { _ ->
                            OppijanumeroException.MalformedResponse(
                                request = request,
                                response = response,
                                cause = decodeError,
                            )
                        },
                    )
            }
}

data class OppijanumerorekisteriHenkilo(
    val oidHenkilo: String?,
    val hetu: String?,
    val kaikkiHetut: List<String>?,
    val passivoitu: Boolean?,
    val etunimet: String?,
    val kutsumanimi: String?,
    val sukunimi: String?,
    val aidinkieli: Kieli?,
    val asiointiKieli: Kieli?,
    val kansalaisuus: List<Kansalaisuus>?,
    val kasittelijaOid: String?,
    val syntymaaika: LocalDate?,
    val sukupuoli: String?,
    val kotikunta: String?,
    val oppijanumero: String?,
    val turvakielto: Boolean?,
    val eiSuomalaistaHetua: Boolean?,
    val yksiloity: Boolean?,
    val yksiloityVTJ: Boolean?,
    val yksilointiYritetty: Boolean?,
    val duplicate: Boolean?,
    val created: OffsetDateTime?,
    val modified: OffsetDateTime?,
    val vtjsynced: OffsetDateTime?,
    val yhteystiedotRyhma: List<Yhteystietoryhma>?,
    val yksilointivirheet: List<Yksilointivirhe>?,
    val passinumero: List<String>?,
) {
    fun hetut() = listOfNotNull(hetu) + (kaikkiHetut ?: emptyList())

    data class Kieli(
        val kieliKoodi: String?,
        val kieliTyyppi: String?,
    )

    data class Kansalaisuus(
        val kansalaisuusKoodi: String?,
    )

    data class Yhteystietoryhma(
        val id: Long?,
        val ryhmaKuvaus: String?,
        val ryhmaAlkuperaTieto: String?,
        val readOnly: Boolean?,
        val yhteystieto: List<Yhteystieto>?,
    ) {
        data class Yhteystieto(
            val yhteystietoTyyppi: String?,
            val yhteystietoArvo: String?,
        )
    }

    data class Yksilointivirhe(
        val yksilointivirheTila: String?,
        val uudelleenyritysAikaleima: OffsetDateTime?,
    )
}
