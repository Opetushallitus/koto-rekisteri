package fi.oph.kitu.oppijanumero

class OppijanumeroServiceMock(
    private val statusCode: Int,
    private val response: YleistunnisteHaeResponse,
) : OppijanumeroService {
    override fun yleistunnisteHae(request: YleistunnisteHaeRequest) = Pair(statusCode, response)
}
