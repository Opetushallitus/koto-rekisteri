package fi.oph.kitu.vkt

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.Oid
import fi.oph.kitu.defaultObjectMapper
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.koski.KoskiRequestMapper
import fi.oph.kitu.schema.SchemaTests
import fi.oph.kitu.tiedonsiirtoschema.Henkilo
import fi.oph.kitu.tiedonsiirtoschema.Henkilosuoritus
import fi.oph.kitu.tiedonsiirtoschema.Lahdejarjestelma
import fi.oph.kitu.tiedonsiirtoschema.LahdejarjestelmanTunniste
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MockMvcResultMatchersDsl
import org.springframework.test.web.servlet.put
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.containers.PostgreSQLContainer
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest
@Import(DBContainerConfiguration::class)
class VktTiedonsiirtoTest {
    @Autowired
    private lateinit var koskiRequestMapper: KoskiRequestMapper

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

    @Test
    fun `Tutkintopaivat siirretaan oikein`() {
        val suoritus =
            Henkilosuoritus(
                Henkilo(Oid.parse("1.2.246.562.10.1234567890").getOrThrow()),
                suoritus =
                    VktSuoritus(
                        taitotaso = Koodisto.VktTaitotaso.HyväJaTyydyttävä,
                        kieli = Koodisto.Tutkintokieli.SWE,
                        suorituksenVastaanottaja = Oid.parse("1.2.246.562.10.1234567890").getOrThrow(),
                        suorituspaikkakunta = "091",
                        osat =
                            listOf(
                                VktPuheenYmmartamisenKoe(
                                    tutkintopaiva = LocalDate.of(2025, 10, 1),
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Tyydyttävä,
                                            paivamaara = LocalDate.of(2025, 10, 1),
                                        ),
                                ),
                                VktTekstinYmmartamisenKoe(
                                    tutkintopaiva = LocalDate.of(2025, 10, 1),
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Tyydyttävä,
                                            paivamaara = LocalDate.of(2025, 10, 1),
                                        ),
                                ),
                                VktKirjoittamisenKoe(
                                    tutkintopaiva = LocalDate.of(2025, 10, 1),
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Tyydyttävä,
                                            paivamaara = LocalDate.of(2025, 10, 1),
                                        ),
                                ),
                                VktPuhumisenKoe(
                                    tutkintopaiva = LocalDate.of(2025, 10, 1),
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Tyydyttävä,
                                            paivamaara = LocalDate.of(2025, 10, 1),
                                        ),
                                ),
                                VktTekstinYmmartamisenKoe(
                                    tutkintopaiva = LocalDate.of(2025, 9, 23),
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Hyvä,
                                            paivamaara = LocalDate.of(2025, 9, 23),
                                        ),
                                ),
                                VktKirjoittamisenKoe(
                                    tutkintopaiva = LocalDate.of(2025, 9, 23),
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Hyvä,
                                            paivamaara = LocalDate.of(2025, 9, 23),
                                        ),
                                ),
                            ),
                        lahdejarjestelmanId =
                            LahdejarjestelmanTunniste(
                                id = "test",
                                lahde = Lahdejarjestelma.KIOS,
                            ),
                    ),
            )

        val koskiRequest = koskiRequestMapper.vktSuoritusToKoskiRequest(suoritus).getOrThrow()
        val pts =
            koskiRequest.opiskeluoikeudet
                .first()
                .suoritukset
                .first()

        fun assertTaito(
            kielitaito: Koodisto.VktKielitaito,
            arvosana: Koodisto.VktArvosana,
            tutkintopaiva: LocalDate,
        ) {
            val taito = pts.osasuoritukset.first { it.koulutusmoduuli.tunniste.koodiarvo == kielitaito.koodiarvo }
            assertEquals(
                arvosana.koodiarvo,
                taito.arviointi
                    .first()
                    .arvosana.koodiarvo,
                "Kielitaidon $kielitaito arvosana ei vastaa",
            )
            assertEquals(
                tutkintopaiva,
                taito.alkamispäivä,
                "Kielitaidon $kielitaito tutkintopäivä ei vastaa",
            )
        }

        assertTaito(
            Koodisto.VktKielitaito.Suullinen,
            Koodisto.VktArvosana.Tyydyttävä,
            LocalDate.of(2025, 10, 1),
        )

        assertTaito(
            Koodisto.VktKielitaito.Ymmärtäminen,
            Koodisto.VktArvosana.Tyydyttävä,
            LocalDate.of(2025, 10, 1),
        )

        assertTaito(
            Koodisto.VktKielitaito.Kirjallinen,
            Koodisto.VktArvosana.Hyvä,
            LocalDate.of(2025, 9, 23),
        )
    }

    private fun putSuoritus(
        suoritus: Henkilosuoritus<*>,
        block: MockMvcResultMatchersDsl.() -> Unit,
    ) {
        putSuoritus(defaultObjectMapper.writeValueAsString(suoritus), block)
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
