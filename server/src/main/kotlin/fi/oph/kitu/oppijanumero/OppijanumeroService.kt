package fi.oph.kitu.oppijanumero

import fi.oph.kitu.oid.Oid
import fi.oph.kitu.result.TypedResult
import fi.oph.kitu.retry.RetryOutboundIntegration
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

interface OppijanumeroService {
    fun getOppijanumero(oppija: Oppija): TypedResult<Oid, OppijanumeroException>

    fun getHenkilo(oid: Oid): TypedResult<OppijanumerorekisteriHenkilo, OppijanumeroException>
}

@Service
@Profile("!test && !e2e && !local-opintopolku")
class OppijanumeroServiceImpl(
    val client: OppijanumerorekisteriClient,
) : OppijanumeroService {
    @WithSpan
    @RetryOutboundIntegration
    override fun getOppijanumero(oppija: Oppija): TypedResult<Oid, OppijanumeroException> {
        require(oppija.etunimet.isNotEmpty()) { "etunimet cannot be empty" }
        require(oppija.hetu.isNotEmpty()) { "hetu cannot be empty" }
        require(oppija.sukunimi.isNotEmpty()) { "sukunimi cannot be empty" }
        require(oppija.kutsumanimi.isNotEmpty()) { "kutsumanimi cannot be empty" }

        val requestBody =
            YleistunnisteHaeRequest(
                oppija.etunimet,
                oppija.hetu,
                oppija.kutsumanimi.split(" ").first(),
                oppija.sukunimi,
            )

        return client
            .onrPost("yleistunniste/hae", requestBody, YleistunnisteHaeResponse::class.java)
            .flatMap { body ->
                val span = Span.current()
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
    @RetryOutboundIntegration
    override fun getHenkilo(oid: Oid): TypedResult<OppijanumerorekisteriHenkilo, OppijanumeroException> =
        client.onrGet("henkilo/$oid", OppijanumerorekisteriHenkilo::class.java)
}
