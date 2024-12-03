package fi.oph.kitu.oppijanumero

class OppijanumeroServiceMock(
    private val oppijanumero: String,
) : OppijanumeroService {
    override fun getOppijanumero(
        etunimet: String,
        hetu: String,
        kutsumanimi: String,
        sukunimi: String,
        oppijanumero: String?,
    ): String = this.oppijanumero
}
