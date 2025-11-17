package fi.oph.kitu.oppijanumero

import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult
import fi.oph.kitu.observability.use
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

interface OppijanumeroService {
    fun getOppijanumero(oppija: Oppija): TypedResult<Oid, OppijanumeroException>

    fun getHenkilo(oid: Oid): TypedResult<OppijanumerorekisteriHenkilo, OppijanumeroException>
}

@Service
@Profile("!test")
class OppijanumeroServiceImpl(
    val tracer: Tracer,
    val client: OppijanumerorekisteriClient,
) : OppijanumeroService {
    override fun getOppijanumero(oppija: Oppija): TypedResult<Oid, OppijanumeroException> =
        tracer
            .spanBuilder("OppijanumeroService.getOppijanumero")
            .startSpan()
            .use { span ->
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
    override fun getHenkilo(oid: Oid): TypedResult<OppijanumerorekisteriHenkilo, OppijanumeroException> =
        client.onrGet("henkilo/$oid", OppijanumerorekisteriHenkilo::class.java)
}
