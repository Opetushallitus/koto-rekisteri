package fi.oph.kitu.oppijanumero

import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult

class OppijanumeroServiceMock(
    private val oppijat: Map<String, String>,
) : OppijanumeroService {
    override fun getOppijanumero(oppija: Oppija): TypedResult<Oid, OppijanumeroException> {
        val oppijanumero = oppijat[oppija.hetu]
        val oppijanumeroResult =
            if (oppijanumero == null) {
                TypedResult.Failure(Error("unknown oppija"))
            } else {
                Oid
                    .parseTyped(oppijanumero)
            }
        return oppijanumeroResult
            .mapFailure {
                OppijanumeroException.MalformedOppijanumero(
                    YleistunnisteHaeRequest(
                        etunimet = oppija.etunimet,
                        hetu = oppija.hetu,
                        kutsumanimi = oppija.kutsumanimi,
                        sukunimi = oppija.sukunimi,
                    ),
                    oppijanumero,
                )
            }
    }
}
