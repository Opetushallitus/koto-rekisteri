package fi.vm.sade.oppijanumerorekisteri.example

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.IOException
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublisher
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

class CasClientJava(
    httpClient: HttpClient,
    baseUrl: String,
) {
    private val log: Logger = LogManager.getLogger(this.javaClass)
    private val httpClient: HttpClient
    private val baseUrl: String

    init {
        log.info("Initializing CasClient for CAS server at {}", baseUrl)
        this.baseUrl = baseUrl
        this.httpClient = httpClient
    }

    fun getTicket(
        username: String,
        password: String,
        service: String,
    ): String {
        try {
            return getServiceTicket(username, password, service)
        } catch (e: IOException) {
            log.error("Failed to get service ticket", e)
            throw RuntimeException(e)
        } catch (e: InterruptedException) {
            log.error("Failed to get service ticket", e)
            throw RuntimeException(e)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun getServiceTicket(
        username: String,
        password: String,
        service: String,
    ): String {
        log.info("Fetching service ticket for service {}...", service)
        val ticketGrantingTicket = getTicketGrantingTicket(username, password)
        val request =
            HttpRequest
                .newBuilder(URI.create("$baseUrl/v1/tickets/$ticketGrantingTicket"))
                .POST(formBody(java.util.Map.of("service", "$service/j_spring_cas_security_check")))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .timeout(Duration.ofSeconds(10))
                .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == 200) {
            val serviceTicket = response.body()
            log.info("Successfully got service ticket: {}", serviceTicket)
            return serviceTicket
        } else {
            val msg = "Failed to get service ticket: " + response.statusCode() + " " + response.body()
            log.error(msg)
            throw RuntimeException(msg)
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun getTicketGrantingTicket(
        username: String,
        password: String,
    ): String {
        log.info("Fetching TGT (Ticket Granting Ticket) from CAS server {}...", baseUrl)
        val request =
            HttpRequest
                .newBuilder(URI.create("$baseUrl/v1/tickets"))
                .POST(formBody(java.util.Map.of("username", username, "password", password)))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == 201) {
            val location = response.headers().firstValue("Location").get()
            val tgt = location.substring(location.lastIndexOf("/") + 1)
            log.info("Successfully fetched TGT (Ticket Granting Ticket): {}", tgt)
            return tgt
        } else {
            val msg = "Failed to get ticket granting ticket: " + response.statusCode() + " " + response.body()
            log.error(msg)
            throw RuntimeException(msg)
        }
    }

    private fun formBody(params: Map<String, String>): BodyPublisher {
        val body = StringBuilder()
        for ((key, value) in params) {
            if (body.length > 0) body.append("&")
            body.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
            body.append("=")
            body.append(URLEncoder.encode(value, StandardCharsets.UTF_8))
        }
        return HttpRequest.BodyPublishers.ofString(body.toString())
    }
}
