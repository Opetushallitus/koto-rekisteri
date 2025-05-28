package fi.oph.kitu.oppijanumero

import fi.oph.kitu.TypedResult
import fi.oph.kitu.printCookies
import fi.oph.kitu.retrieveEntitySafely
import io.opentelemetry.instrumentation.annotations.WithSpan
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.net.CookieManager
import java.net.URI
import java.net.URLEncoder

@Service
class CasRestService(
    @Qualifier("casRestClient")
    val restClient: RestClient,
    val cookieManager: CookieManager,
) {
    @Value("\${kitu.palvelukayttaja.username}")
    private lateinit var onrUsername: String

    @Value("\${kitu.palvelukayttaja.password}")
    private lateinit var onrPassword: String

    @Value("\${kitu.oppijanumero.casUrl}")
    private lateinit var casUrl: String

    @Value("\${kitu.oppijanumero.service.url}")
    private lateinit var serviceUrl: String

    @WithSpan
    fun verifyServiceTicket(serviceTicket: String): TypedResult<URI, CasError> {
        println("sendAuthenticationRequest: $serviceUrl/j_spring_cas_security_check?ticket=$serviceTicket")
        val response =
            restClient
                .get()
                .uri("$serviceUrl/j_spring_cas_security_check?ticket=$serviceTicket")
                .headers {
                    val uri = URI.create(serviceUrl)
                    val cookies = cookieManager.cookieStore.get(uri)
                    val jsessionid = cookies.find { c -> c.name == "JSESSIONID" }?.value

                    it["JSESSIONID"] = jsessionid
                }.exchange { request, response ->
                    println(" - Sending request headers:")
                    request.headers.forEach {
                        println("   -- ${it.key}: ${it.value.joinToString(", ")}")
                    }

                    println(" - Receiving response headers:")
                    request.headers.forEach {
                        println("   -- ${it.key}: ${it.value.joinToString(", ")}")
                    }

                    response
                }

        val body = response?.bodyTo(String::class.java)

        cookieManager.printCookies()
        println(" - Response:")
        println("   -- StatusCode: ${response?.statusCode ?: "<null>"}")
        println("   -- Response: ${body ?: "<null>"}")
        println("   -- Location: ${response?.headers?.location ?: "<null>"}")

        return if (response?.statusCode == HttpStatus.FOUND && response.headers.location != null) {
            TypedResult.Success(response.headers.location!!)
        } else {
            // TODO: Improve failure. Create new CasError, don't use ServiceTicket
            TypedResult.Failure(CasError.ServiceTicketError("Received status code ${response?.statusCode}"))
        }
    }

    @WithSpan
    fun getServiceTicket(ticketGrantingTicket: String): TypedResult<String, CasError> {
        // Step 3 - Get the response
        println(
            "getServiceTicket: $casUrl/v1/tickets/$ticketGrantingTicket - body: service=$serviceUrl/j_spring_cas_security_check",
        )
        val response =
            restClient
                .post()
                .uri("$casUrl/v1/tickets/$ticketGrantingTicket")
                .body("service=$serviceUrl/j_spring_cas_security_check")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieveEntitySafely<String>()

        cookieManager.printCookies("getGrantingTicket\n")
        println(" - ServiceTicket: ${response?.body}")

        return if (response?.statusCode == HttpStatus.OK) {
            TypedResult.Success(response.body.toString())
        } else {
            TypedResult.Failure(CasError.ServiceTicketError("Unable to get service ticket"))
        }
    }

    @WithSpan
    fun getGrantingTicket(): TypedResult<String, CasError> {
        // Step 2 - form a request
        val username = URLEncoder.encode(onrUsername, "UTF-8")
        val password = URLEncoder.encode(onrPassword, "UTF-8")
        val body = "username=$username&password=$password"

        println("getGrantingTicket: $casUrl/v1/tickets")
        val response =
            restClient
                .post()
                .uri("$casUrl/v1/tickets")
                .body(body)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieveEntitySafely<String>()

        val ticket =
            response
                ?.headers
                ?.location
                ?.path
                ?.substringAfterLast("/")

        cookieManager.printCookies("getGrantingTicket\n")
        println(" - GrantingTicket: $ticket")

        return if (response?.statusCode == HttpStatus.CREATED && ticket != null) {
            TypedResult.Success(ticket)
        } else {
            TypedResult.Failure(CasError.GrantingTicketError("Unable to get granting ticket"))
        }
    }
}
