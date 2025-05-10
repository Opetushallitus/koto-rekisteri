package fi.oph.kitu.oppijanumero

import fi.oph.kitu.TypedResult
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class CasAuthenticatedServiceMock(
    private val mockResponse: TypedResult<HttpResponse<String>, CasError>,
) : CasAuthenticatedService {
    override fun sendRequest(requestBuilder: HttpRequest.Builder): TypedResult<HttpResponse<String>, CasError> =
        mockResponse
}
