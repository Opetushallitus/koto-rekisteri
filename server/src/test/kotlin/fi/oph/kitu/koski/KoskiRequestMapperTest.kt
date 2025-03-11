package fi.oph.kitu.koski

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import fi.oph.kitu.mock.generateRandomYkiSuoritusEntity
import fi.oph.kitu.yki.Tutkintokieli
import fi.oph.kitu.yki.Tutkintotaso
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import java.time.LocalDate
import kotlin.test.assertEquals

class KoskiRequestMapperTest {
    private val koskiRequestMapper = KoskiRequestMapper()
    private val objectMapper = jacksonObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())

    @Test
    fun `map yki suoritus to koski request`() {
        val suoritus =
            generateRandomYkiSuoritusEntity().copy(
                suorittajanOID = "1.2.246.562.24.12345678910",
                suoritusId = 123456,
                tutkintopaiva = LocalDate.of(2025, 1, 1),
                arviointipaiva = LocalDate.of(2025, 1, 3),
                tutkintotaso = Tutkintotaso.PT,
                tutkintokieli = Tutkintokieli.ENG,
                jarjestajanTunnusOid = "1.2.246.562.10.12345678910",
                tekstinYmmartaminen = 2,
                kirjoittaminen = 2,
                puheenYmmartaminen = 2,
                puhuminen = 2,
                rakenteetJaSanasto = null,
                yleisarvosana = null,
            )
        val koskiRequest = koskiRequestMapper.ykiSuoritusToKoskiRequest(suoritus)
        val expectedJson =
            objectMapper
                .readValue(
                    ClassPathResource("./koski-request-example.json").file,
                    JsonNode::class.java,
                ).toString()
        val koskiRequestJson = objectMapper.writeValueAsString(koskiRequest)
        assertEquals(expectedJson, koskiRequestJson)
    }

    @Test
    fun `map yki suoritus with yleisarvosana to koski request`() {
        val suoritus =
            generateRandomYkiSuoritusEntity().copy(
                suorittajanOID = "1.2.246.562.24.12345678910",
                suoritusId = 123456,
                tutkintopaiva = LocalDate.of(2025, 1, 1),
                arviointipaiva = LocalDate.of(2025, 1, 3),
                tutkintotaso = Tutkintotaso.PT,
                tutkintokieli = Tutkintokieli.ENG,
                jarjestajanTunnusOid = "1.2.246.562.10.12345678910",
                tekstinYmmartaminen = 2,
                kirjoittaminen = 2,
                puheenYmmartaminen = 2,
                puhuminen = 2,
                rakenteetJaSanasto = null,
                yleisarvosana = 2,
            )
        val koskiRequest = koskiRequestMapper.ykiSuoritusToKoskiRequest(suoritus)
        val expectedJson =
            objectMapper
                .readValue(
                    ClassPathResource("./koski-request-with-yleisarvosana.json").file,
                    JsonNode::class.java,
                ).toString()
        val koskiRequestJson = objectMapper.writeValueAsString(koskiRequest)
        assertEquals(expectedJson, koskiRequestJson)
    }
}
