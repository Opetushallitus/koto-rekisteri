package fi.oph.kitu.apidocs

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.defaultObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.web.client.RestTemplate
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(DBContainerConfiguration::class)
class OpenApiSpecTest(
    @param:Autowired private val postgres: PostgreSQLContainer,
) {
    @LocalServerPort
    private var port: Int = 0

    private val restTemplate = RestTemplate()

    // Springdoc 3.0.x palauttaa /v3/api-docs:n byte[]:nä; SB4:ssä Jackson 3 ehti
    // serialisoida sen base64-merkkijonoksi (Jackson byte[]-oletus), jolloin Swagger UI
    // näytti "no valid version field" -virheen. MessageConverterConfig prependoi
    // ByteArrayHttpMessageConverterin, jotta byte[] menee läpi raakana.
    @Test
    fun `GET v3 api-docs returns valid OpenAPI JSON, not base64`() {
        val body =
            requireNotNull(
                restTemplate.getForObject(
                    "http://localhost:$port/v3/api-docs",
                    String::class.java,
                ),
            ) { "Empty response from /v3/api-docs" }

        assertTrue(
            body.trimStart().startsWith("{"),
            "Expected raw JSON object, got: ${body.take(120)}",
        )

        val openapi = defaultObjectMapper.readTree(body)["openapi"]?.asString()
        assertEquals(
            true,
            openapi?.matches(Regex("^3\\.\\d+\\.\\d+$")),
            "Expected openapi version 3.x.x, got: $openapi",
        )
    }
}
