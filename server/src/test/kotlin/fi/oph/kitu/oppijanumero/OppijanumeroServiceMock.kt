package fi.oph.kitu.oppijanumero

import fi.oph.kitu.TypedResult
import fi.oph.kitu.TypedResult.Success

class OppijanumeroServiceMock(
    private val oppijanumero: String,
) : OppijanumeroService {
    override fun getOppijanumero(oppija: Oppija): TypedResult<String, OppijanumeroException> =
        Success(this.oppijanumero)
}
