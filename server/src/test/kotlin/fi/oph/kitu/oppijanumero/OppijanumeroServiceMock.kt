package fi.oph.kitu.oppijanumero

class OppijanumeroServiceMock(
    private val statusCode: Int,
    private val body: String,
) : OppijanumeroService {
    override fun yleistunnisteHae(request: YleistunnisteHaeRequest) = Pair(statusCode, body)
}
