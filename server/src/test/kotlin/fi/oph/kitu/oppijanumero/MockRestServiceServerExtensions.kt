package fi.oph.kitu.oppijanumero

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.net.URI
import kotlin.text.contains

fun MockRestServiceServer.isNotLoggedInToCas(serviceUrl: String) {
    this
        .expect(requestTo(serviceUrl))
        .andExpect { request ->
            val cookieHeader = request.headers.getFirst("Cookie")
            if (cookieHeader != null && cookieHeader.contains("JSESSIONID=")) {
                throw AssertionError("Unexpected JSESSIONID cookie present")
            }
        }.andRespond(
            withStatus(HttpStatus.FOUND)
                .location(URI("http://localhost:8080/cas/v1/cas/login")),
        )
}

fun MockRestServiceServer.setupCasGrantingTicket(casBaseUrl: String) {
    this
        .expect(requestTo("$casBaseUrl/v1/tickets"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string("username=username&password=password"))
        .andRespond(
            withStatus(HttpStatus.CREATED)
                .header("Location", "http://localhost:8080/v1/tickets/12345-6"),
        )
}

fun MockRestServiceServer.setupCasServiceTicket(casBaseUrl: String) {
    this
        .expect(requestTo("$casBaseUrl/v1/tickets/12345-6"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
        .andExpect(content().string("service=$casBaseUrl/login/j_spring_cas_security_check"))
        .andRespond(
            withSuccess(
                "65432-1",
                MediaType.TEXT_HTML,
            ),
        )
}

fun MockRestServiceServer.setupCasVerifyTicket(
    serviceBaseUrl: String,
    endpoint: String,
) {
    val serviceUrl = "$serviceBaseUrl/$endpoint"
    this
        .expect(requestTo("$serviceBaseUrl/j_spring_cas_security_check?ticket=65432-1"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withStatus(HttpStatus.FOUND)
                .header("Location", serviceUrl),
        )
}

/**
 * Setups CAS login flow to the instance of `MockRestServiceServer`.
 *
 * Returns generated `RestClient.Builder` that CAS login flow uses.
 */
fun MockRestServiceServer.addCasAuthenticatedService(
    serviceBaseUrl: String,
    serviceEndpoint: String,
    respondWithJsonBody: String,
): RestClient.Builder {
    val casBaseUrl = "http://localhost:8080/cas"
    val serviceUrl = "$serviceBaseUrl/$serviceEndpoint"

    val builder = RestClient.builder().baseUrl(casBaseUrl)
    val casMockServer =
        MockRestServiceServer
            .bindTo(builder)
            .ignoreExpectOrder(true)
            .build()

    // Expectation 1: App tries to use CAS-authenticated site.
    // It does not have the correct cookie, so CAS returns HTTP 302
    this.isNotLoggedInToCas(serviceUrl)

    // Expectation 2: getGrantingTicket - Since CAS provided login - site, now we have to get granting ticket:
    casMockServer.setupCasGrantingTicket(casBaseUrl)

    // Expectation 3: getServiceTicket - After granting ticket, -> service ticket
    casMockServer.setupCasServiceTicket(casBaseUrl)

    // Exepectation 4: verifyServiceTicket - service ticket validated in CAS Authenticated service
    this.setupCasVerifyTicket(serviceBaseUrl, serviceEndpoint)

    // Expectation 5: Authenticated request
    this
        .expect(requestTo(serviceUrl))
        .andRespond(withSuccess(respondWithJsonBody, MediaType.APPLICATION_JSON))
    return builder
}
