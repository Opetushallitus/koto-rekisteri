package fi.oph.kitu.oppijanumero

import com.fasterxml.jackson.databind.ObjectMapper
import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.observability.use
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class OppijanumeroService(
    val tracer: Tracer,
    val client: OppijanumerorekisteriClient,
) {
    fun getOppijanumero(oppija: Oppija): TypedResult<Oid, OppijanumeroException> =
        tracer
            .spanBuilder("OppijanumeroService.getOppijanumero")
            .startSpan()
            .use { span ->
                require(oppija.etunimet.isNotEmpty()) { "etunimet cannot be empty" }
                require(oppija.hetu.isNotEmpty()) { "hetu cannot be empty" }
                require(oppija.sukunimi.isNotEmpty()) { "sukunimi cannot be empty" }
                require(oppija.kutsumanimi.isNotEmpty()) { "kutsumanimi cannot be empty" }

                val requestBody =
                    YleistunnisteHaeRequest(oppija.etunimet, oppija.hetu, oppija.kutsumanimi, oppija.sukunimi)

                client
                    .onrPost("yleistunniste/hae", requestBody, YleistunnisteHaeResponse::class.java)
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

    @WithSpan
    fun getHenkilo(oid: Oid): TypedResult<OppijanumerorekisteriHenkilo, OppijanumeroException> =
        client.onrGet("henkilo/$oid", OppijanumerorekisteriHenkilo::class.java)
}

@Service
class OppijanumerorekisteriClient(
    val casAuthenticatedService: CasAuthenticatedService,
    val objectMapper: ObjectMapper,
) {
    @Value("\${kitu.oppijanumero.service.url}")
    lateinit var serviceUrl: String

    @WithSpan
    fun <T> onrGet(
        endpoint: String,
        responseType: Class<T>,
    ) = fetch<T, EmptyRequest>(HttpMethod.GET, endpoint, responseType = responseType)

    @WithSpan
    fun <T, R : OppijanumerorekisteriRequest> onrPost(
        endpoint: String,
        body: R,
        clazz: Class<T>,
    ) = fetch<T, R>(HttpMethod.POST, endpoint, body, clazz)

    @WithSpan
    fun <T, R : OppijanumerorekisteriRequest> fetch(
        httpMethod: HttpMethod,
        endpoint: String,
        body: OppijanumerorekisteriRequest? = null,
        responseType: Class<T>,
    ): TypedResult<T, OppijanumeroException> {
        val url = "$serviceUrl/$endpoint"

        // no need to log sendRequest, because there are request and response logging inside casAuthenticatedService.
        val rawResult =
            casAuthenticatedService.fetch(httpMethod, url, body, MediaType.APPLICATION_JSON, String::class.java)
        if (rawResult !is TypedResult.Success) {
            // CAS errors are not caused by the oppija data, and thus
            // should be handling outside default error handling flow.
            throw (rawResult as TypedResult.Failure).error
        }

        // At this point, CAS-authentication is done succesfully,
        // but we still need to check endpoint specific statuses
        val rawResponse = rawResult.value
        if (rawResponse.statusCode == HttpStatus.NOT_FOUND) {
            return TypedResult.Failure(OppijanumeroException.OppijaNotFoundException(body ?: EmptyRequest()))
        } else if (rawResponse.statusCode.is4xxClientError) {
            return TypedResult.Failure(OppijanumeroException.BadRequest(body ?: EmptyRequest(), rawResponse))
        } else if (!rawResponse.statusCode.is2xxSuccessful) {
            throw OppijanumeroException.UnexpectedError(body ?: EmptyRequest(), rawResponse)
        }

        return deserializeResponse(body ?: EmptyRequest(), rawResult.value, responseType)
    }

    /**
     * Tries to convert `HttpResponse<String>` into the given `T`.
     * If the conversion fails, it checks whether the response was OppijanumeroServiceError.
     * In that case [OppijanumeroException.BadResponse] will be thrown.
     * Otherwise, the underlying exception will be thrown
     */
    @WithSpan
    fun <T> deserializeResponse(
        request: OppijanumerorekisteriRequest,
        response: ResponseEntity<String>,
        clazz: Class<T>,
    ): TypedResult<T, OppijanumeroException> =
        TypedResult
            .runCatching {
                objectMapper.readValue(response.body, clazz)
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
    val passinumerot: List<String>?,
) {
    fun hetut() = listOfNotNull(hetu) + (kaikkiHetut.orEmpty())

    fun kokoNimi() = "$etunimet $sukunimi"

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
