package fi.oph.kitu.vkt

import fi.oph.kitu.schema.Henkilosuoritus
import fi.oph.kitu.schema.SchemaTests
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MockMvcResultMatchersDsl
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import kotlin.test.Test

@SpringBootTest
@Testcontainers
class VktTiedonsiirtoTest {
    @Autowired
    private lateinit var context: WebApplicationContext
    private var mockMvc: MockMvc? = null

    @Suppress("unused")
    companion object {
        @JvmStatic
        @Container
        @ServiceConnection
        val postgres =
            PostgreSQLContainer("postgres:16")
                .withUrlParam("stringtype", "unspecified")!!
    }

    @BeforeEach
    fun setup() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply { springSecurity() }
                .build()
    }

    @Test
    fun `saving valid suoritus works`() {
        putSuoritus(SchemaTests.vktHenkilosuoritus) {
            status { isOk() }
            jsonPath("$.result") { value("OK") }
        }
    }

    @Test
    fun `saving suoritus with missing fields does not work`() {
        val json =
            """
            {
              "henkilo": {
                "oid": "1.2.246.562.10.1234567890",
                "etunimet": "Kalle",
                "sukunimi": "Testaaja"
              },
              "suoritus": {
                "kieli": "FI",
                "osakokeet": [],
                "lahdejarjestelmanId": {
                  "id": "404",
                  "lahde": "KIOS"
                },
                "tyyppi": "valtionhallinnonkielitutkinto"
              }
            }
            """.trimIndent()
        putSuoritus(json) {
            status { isBadRequest() }
            jsonPath("$.result") { value("Failed") }
            jsonPath("$.errors[0]") { exists() }
        }
    }

    private fun putSuoritus(
        suoritus: Henkilosuoritus<*>,
        block: MockMvcResultMatchersDsl.() -> Unit,
    ) {
        putSuoritus(Henkilosuoritus.getDefaultObjectMapper().writeValueAsString(suoritus), block)
    }

    private fun putSuoritus(
        suoritusJson: String,
        block: MockMvcResultMatchersDsl.() -> Unit,
    ): MockHttpServletResponse =
        mockMvc!!
            .put("/api/vkt/kios") {
                contentType = MediaType.APPLICATION_JSON
                accept = MediaType.APPLICATION_JSON
                content = suoritusJson
            }.andExpect {
                content { contentType(MediaType.APPLICATION_JSON) }
                block()
            }.andReturn()
            .response
}
