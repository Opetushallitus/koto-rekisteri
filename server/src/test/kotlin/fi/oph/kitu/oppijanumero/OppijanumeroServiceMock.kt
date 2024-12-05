package fi.oph.kitu.oppijanumero

class OppijanumeroServiceMock(
    private val oppijanumero: String,
    private val oppijanumeroException: OppijanumeroException? = null,
) : OppijanumeroService {
    override fun getOppijanumeroOrError(oppija: Oppija): Pair<OppijanumeroException?, String?> =
        Pair(oppijanumeroException, oppijanumero)

    override fun getOppijanumero(oppija: Oppija): String = oppijanumero
}
