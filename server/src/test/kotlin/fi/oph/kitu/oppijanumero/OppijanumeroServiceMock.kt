package fi.oph.kitu.oppijanumero

class OppijanumeroServiceMock(
    private val oppijanumero: String,
) : OppijanumeroService {
    override fun getOppijanumero(oppija: Oppija) = Result.success(this.oppijanumero)
}
