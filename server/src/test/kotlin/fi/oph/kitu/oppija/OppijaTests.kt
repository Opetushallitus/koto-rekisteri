package fi.oph.kitu.oppija

import fi.oph.kitu.test.DBFixture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.servlet.client.MockMvcWebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OppijaTests(
    @Autowired val controller: OppijaController,
) : DBFixture() {
    private lateinit var client: WebTestClient

    @BeforeEach
    fun setupClient() {
        client = MockMvcWebTestClient.bindToController(controller).build()
    }

    @Test
    fun `get oppija`() {
        client
            .get()
            .uri("/api/oppija")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json("[]")
    }

    @Test
    fun `post oppija`() {
        client
            .post()
            .uri("/api/oppija")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(
                "{" +
                    "\"oid\": \"1.2.246.562.24.12345678910\"," +
                    "\"lastName\": \"Ykittäjä\"," +
                    "\"firstName\": \"Yrjö\"," +
                    "\"hetu\": \"010106A911C\"," +
                    "\"nationality\": \"GBR\"," +
                    "\"gender\": \"E\"," +
                    "\"address\": \"Hakaniemenranta 6\"," +
                    "\"postalCode\": \"00530\"," +
                    "\"city\": \"Helsinki\"," +
                    "\"email\": \"kirjaamo@oph.fi\"" +
                    "}",
            ).exchange()
            .expectStatus()
            .isCreated()
            .expectBody()
            .json(
                "{" +
                    "\"oid\": \"1.2.246.562.24.12345678910\"," +
                    "\"lastName\": \"Ykittäjä\"," +
                    "\"firstName\": \"Yrjö\"," +
                    "\"hetu\": \"010106A911C\"," +
                    "\"nationality\": \"GBR\"," +
                    "\"gender\": \"E\"," +
                    "\"address\": \"Hakaniemenranta 6\"," +
                    "\"postalCode\": \"00530\"," +
                    "\"city\": \"Helsinki\"," +
                    "\"email\": \"kirjaamo@oph.fi\"" +
                    "}",
            )
    }
}
