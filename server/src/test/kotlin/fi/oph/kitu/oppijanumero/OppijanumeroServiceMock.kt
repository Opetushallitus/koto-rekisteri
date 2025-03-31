package fi.oph.kitu.oppijanumero

import fi.oph.kitu.Oid
import fi.oph.kitu.TypedResult

class OppijanumeroServiceMock(
    private val oppijanumero: String,
) : OppijanumeroService {
    override fun getOppijanumero(oppija: Oppija): TypedResult<Oid, OppijanumeroException> =
        Oid
            .parseTyped(oppijanumero)
            .mapFailure { OppijanumeroException.MalformedOppijanumero(oppija, oppijanumero) }
}
