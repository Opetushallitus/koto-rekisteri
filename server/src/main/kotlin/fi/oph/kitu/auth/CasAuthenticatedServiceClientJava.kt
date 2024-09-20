package fi.vm.sade.oppijanumerorekisteri.example

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

abstract class CasAuthenticatedServiceClientJava protected constructor(
    httpClient: HttpClient,
    casClientJava: CasClientJava,
    serviceUrl: String,
) {
    private val log: Logger = LogManager.getLogger(this.javaClass)
    protected val httpClient: HttpClient
    private val casClientJava: CasClientJava
    protected val serviceUrl: String

    init {
        log.info("Initializing CasAuthenticatedServiceClient for service {}", serviceUrl)
        this.httpClient = httpClient
        this.casClientJava = casClientJava
        this.serviceUrl = serviceUrl
    }

    @Throws(IOException::class, InterruptedException::class)
    protected fun sendRequest(requestBuilder: HttpRequest.Builder): HttpResponse<String> {
        log.info("Sending CAS authenticated request")
        requestBuilder
            .timeout(Duration.ofSeconds(10))
            // .header("Caller-Id", Config.callerId)
            .header("CSRF", "CSRF")
            .header("Cookie", "CSRF=CSRF")
        val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())

        if (isLoginToCas(response)) {
            // Oppijanumerorekisteri ohjaa CAS kirjautumissivulle, jos autentikaatiota
            // ei ole tehty. Luodaan uusi CAS ticket ja yritetään uudelleen.
            log.info("Was redirected to CAS login")
            authenticateWithJSpringCasSecurityCheckEndpoint()
            return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        } else if (response.statusCode() == 401) {
            // Oppijanumerorekisteri vastaa HTTP 401 kun sessio on vanhentunut.
            // HUOM! Oppijanumerorekisteri vastaa HTTP 401 myös jos käyttöoikeudet eivät riitä.
            log.info("Received HTTP 401 response")
            authenticateWithJSpringCasSecurityCheckEndpoint()
            return httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString())
        } else {
            return response
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun authenticateWithJSpringCasSecurityCheckEndpoint() {
        val uri = URI.create(serviceUrl + "/j_spring_cas_security_check?ticket=" + fetchCasServiceTicket())
        val authRequest =
            HttpRequest
                .newBuilder(uri)
                .method("GET", HttpRequest.BodyPublishers.noBody())
                .build()
        val authResponse = httpClient.send(authRequest, HttpResponse.BodyHandlers.ofString())
        log.info("Auth reset response: {}", authResponse)
    }

    private fun fetchCasServiceTicket(): String {
        log.info("Refreshing CAS ticket")
        return casClientJava.getTicket(
            "", // Config.palvelukayttajaUsername,
            "", // Config.palvelukayttajaPassword,
            serviceUrl,
        )
    }

    private fun isLoginToCas(response: HttpResponse<*>): Boolean {
        if (response.statusCode() == 302) {
            val header = response.headers().firstValue("Location")
            return header.map { location: String -> location.contains("/cas/login") }.orElse(false)
        }
        return false
    }
}
