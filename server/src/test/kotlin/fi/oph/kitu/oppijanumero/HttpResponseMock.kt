import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Optional
import javax.net.ssl.SSLSession

class HttpResponseMock(
    private val statusCode: Int,
    private val body: String,
) : HttpResponse<String> {
    override fun statusCode(): Int = statusCode

    override fun body(): String = body

    override fun headers(): HttpHeaders = throw NotImplementedError()

    override fun request(): HttpRequest = throw NotImplementedError()

    override fun previousResponse(): Optional<HttpResponse<String>> = throw NotImplementedError()

    override fun sslSession(): Optional<SSLSession> = throw NotImplementedError()

    override fun uri(): URI = throw NotImplementedError()

    override fun version(): HttpClient.Version = throw NotImplementedError()
}
