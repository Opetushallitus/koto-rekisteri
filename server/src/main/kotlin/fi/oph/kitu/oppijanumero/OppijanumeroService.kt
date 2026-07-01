package fi.oph.kitu.oppijanumero

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.raise.either
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.util.retry.RetryOutboundIntegration
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

interface OppijanumeroService {
    fun getOppijanumero(oppija: Oppija): Either<OppijanumeroException, Oid>

    fun getHenkilo(oid: Oid): Either<OppijanumeroException, OppijanumerorekisteriHenkilo>

    fun getMasterOid(henkiloOid: Oid): Either<OppijanumeroException, Oid> =
        either {
            val henkilo = getHenkilo(henkiloOid).bind()
            parseOid(henkilo.oppijanumero ?: henkilo.oidHenkilo).bind()
        }

    fun parseOid(source: String?): Either<OppijanumeroException, Oid> =
        Oid
            .parse(source)
            .mapLeft { OppijanumeroException.MalformedOppijanumero(oppijanumero = source) }
}

@Service
@Profile("!test && !e2e && !local-opintopolku")
class OppijanumeroServiceImpl(
    val client: OppijanumerorekisteriClient,
) : OppijanumeroService {
    @WithSpan
    @RetryOutboundIntegration
    override fun getOppijanumero(oppija: Oppija): Either<OppijanumeroException, Oid> {
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
                    OppijanumeroException.OppijaNotIdentifiedException(requestBody).left()
                } else {
                    Oid
                        .parse(body.oppijanumero)
                        .mapLeft {
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
    override fun getHenkilo(oid: Oid): Either<OppijanumeroException, OppijanumerorekisteriHenkilo> =
        client.onrGet("henkilo/$oid", OppijanumerorekisteriHenkilo::class.java)
}
