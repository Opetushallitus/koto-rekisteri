package fi.oph.kitu.oppijanumerorekisteri

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class OppijanumerorekisteriService(
    private val restTemplate: RestTemplate,
) {
    @Value("\${kitu.kielitesti.oppijanumerorekisteri.username}")
    lateinit var onrUsername: String

    @Value("\${kitu.kielitesti.oppijanumerorekisteri.password}")
    lateinit var onrPassword: String

    fun httpPostOnCasEndpoint(requestData: YleistunnisteHaeRequest): ResponseEntity<String> {
        val baseUrl = "https://virkailija.testiopintopolku.fi"
        val haeYleistunnisteUrl = "$baseUrl/oppijanumerorekisteri-service/yleistunniste/hae"
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers["Caller-Id"] = "" // TODO: Caller-Id
        headers["CSRF"] = "CSRF" // TODO: CSRF
        headers["Cookie"] = "CSRF=CSRF" // TODO: Cookie
        val request = HttpEntity(requestData, headers)

        // first attempt
        val response =
            restTemplate
                .postForEntity(haeYleistunnisteUrl, request, String::class.java)

        // isRedirectToLoginPage = true:
        //      Oppijanumerorekisteri ohjaa CAS kirjautumissivulle, jos autentikaatiota
        //      ei ole tehty. Luodaan uusi CAS ticket ja yritetään uudelleen.
        //
        // response.statusCode == HttpStatus.UNAUTHORIZED
        //      Oppijanumerorekisteri vastaa HTTP 401 kun sessio on vanhentunut.
        //      HUOM! Oppijanumerorekisteri vastaa HTTP 401 myös jos käyttöoikeudet eivät riitä.
        val loginRequired = isRedirectToLoginPage(response) || response.statusCode == HttpStatus.UNAUTHORIZED
        if (loginRequired) {
            doLogin()

            // try again
            return restTemplate.postForEntity(haeYleistunnisteUrl, request, String::class.java)
        }

        // the authentication had been done before, so the first request when through.
        return response
    }

    // same than:
    // https://github.com/Opetushallitus/oppijanumerorekisteri/blob/master/example/src/main/java/fi/vm/sade/oppijanumerorekisteri/example/CasAuthenticatedServiceClient.java#L69-L75
    private fun isRedirectToLoginPage(response: ResponseEntity<String>): Boolean {
        val location = response.headers["Location"]?.firstOrNull()
        return location != null && location.contains("/cas/login")
    }

    // CasAuthenticatedServiceClient: authenticateWithJSpringCasSecurityCheckEndpoint
    fun doLogin() {
        val ticket = getTicket()
        val response =
            restTemplate.getForEntity(
                "https://virkailija.testiopintopolku.fi/cas/j_spring_cas_security_check?ticket=$ticket",
                String::class.java,
            )

        print("login done successfully")
        println(response.body)
    }

    // CasClient.java - private String getServiceTicket
    fun getTicket(): String? {
        data class RequestBody(
            val service: String,
        )
        val ticket = getGrantingTicket()

        val response =
            restTemplate.postForEntity(
                "https://virkailija.testiopintopolku.fi/cas/v1/ticket=$ticket",
                HttpEntity(
                    RequestBody("https://virkailija.testiopintopolku.fi/cas/j_spring_cas_security_check"),
                    HttpHeaders().apply {
                        contentType = MediaType.APPLICATION_FORM_URLENCODED
                        // TODO: 10 seconds timeout
                    },
                ),
                String::class.java,
            )

        if (response.statusCode != HttpStatus.OK) {
            throw Error("Failed to get service ticket - StatusCode:${response.statusCode}, Message: ${response.body}")
        }

        return response.body
    }

    // CasClient.java - private String getTicketGrantingTicket
    fun getGrantingTicket(): String? {
        data class RequestBody(
            val username: String,
            val password: String,
        )

        val username = onrUsername
        val password = onrPassword
        println("Username: $username, password: $password")

        val request =
            HttpEntity(
                "username=$username&password=$password",
                HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_FORM_URLENCODED
                },
            )

        val response =
            restTemplate.postForEntity(
                // "https://virkailija.testiopintopolku.fi/cas/v1/tickets",
                "http://localhost:80/post",
                request,
                String::class.java,
            )

        println("response: ${response.body}")
        if (response.statusCode != HttpStatus.CREATED) {
            throw Error("Failed to get granting ticket - StatusCode:${response.statusCode}, Message: ${response.body}")
        }

        return response.headers["Location"]
            ?.firstOrNull()
            ?.substringAfterLast("/")
    }
}
