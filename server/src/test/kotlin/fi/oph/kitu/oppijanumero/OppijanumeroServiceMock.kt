package fi.oph.kitu.oppijanumero

import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult

class OppijanumeroServiceMock(
    private val oppijat: Map<String, TypedResult<Oid, OppijanumeroException>>,
) : OppijanumeroService {
    override fun getOppijanumero(oppija: Oppija): TypedResult<Oid, OppijanumeroException> =
        oppijat[oppija.hetu] ?: TypedResult.Failure(
            OppijanumeroException.OppijaNotFoundException(
                YleistunnisteHaeRequest(
                    etunimet = oppija.etunimet,
                    hetu = oppija.hetu,
                    kutsumanimi = oppija.kutsumanimi,
                    sukunimi = oppija.sukunimi,
                ),
            ),
        )

    override fun getHenkilo(oid: Oid): TypedResult<OppijanumerorekisteriHenkilo, OppijanumeroException> {
        TODO("Not yet implemented")
    }
}
