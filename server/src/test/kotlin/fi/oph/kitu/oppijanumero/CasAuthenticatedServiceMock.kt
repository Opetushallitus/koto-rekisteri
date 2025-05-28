package fi.oph.kitu.oppijanumero

import fi.oph.kitu.TypedResult
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import java.net.URI

class CasAuthenticatedServiceMock(
    private val mockResponse: TypedResult<ResponseEntity<String>, CasError>,
) : CasAuthenticatedService {
    override fun <Request : Any, Response : Any> authenticatedPost(
        uri: URI,
        body: Request,
        contentType: MediaType,
        responseType: Class<Response>,
    ): TypedResult<ResponseEntity<Response>, CasError> = mockResponse as TypedResult<ResponseEntity<Response>, CasError>
}
