package fi.oph.kitu.oppijanumero

import java.net.http.HttpRequest
import java.net.http.HttpResponse

class CasAuthenticatedServiceMock(
    private val mockResponse: Result<HttpResponse<String>>,
) : CasAuthenticatedService {
    override fun sendRequest(requestBuilder: HttpRequest.Builder): Result<HttpResponse<String>> = mockResponse
}
