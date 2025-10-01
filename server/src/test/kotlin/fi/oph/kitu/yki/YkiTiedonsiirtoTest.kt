package fi.oph.kitu.yki

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.defaultObjectMapper
import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.Lahdejarjestelma
import fi.oph.kitu.yki.suoritukset.YkiSuoritus
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.PostgreSQLContainer
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@Import(DBContainerConfiguration::class)
class YkiTiedonsiirtoTest {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired private var postgres: PostgreSQLContainer<*>? = null
    private var mockMvc: MockMvc? = null

    @BeforeEach
    fun setup() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply { springSecurity() }
                .build()
    }

    @Test
    fun `YKI-json deserialisoituu Henkilosuoritukseksi`() {
        val json = ClassPathResource("./yki-tiedonsiirto-example.json").file
        val data = defaultObjectMapper.readValue(json, Henkilosuoritus::class.java)
        val suoritus = data.suoritus as YkiSuoritus

        assertEquals("010180-9026", data.henkilo.hetu)
        assertEquals(Lahdejarjestelma.Solki, suoritus.lahdejarjestelmanId.lahde)
        assertEquals(6, suoritus.osat.size)
    }
}
