package fi.oph.kitu.vkt

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.Oid
import fi.oph.kitu.defaultObjectMapper
import fi.oph.kitu.isBadRequest
import fi.oph.kitu.isOk
import fi.oph.kitu.koodisto.Koodisto
import fi.oph.kitu.koski.KoskiRequestMapper
import fi.oph.kitu.koski.VktKielitaito
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
import kotlin.test.assertTrue

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
            isOk()
        }
    }

    @Test
    fun `saving suoritus with missing fields does not work`() {
        val json =
            """
            {
              "henkilo": {
                "oid": "1.2.246.562.24.10691606777",
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
            isBadRequest(
                "JSON parse error: Instantiation of [simple type, class fi.oph.kitu.vkt.VktSuoritus] value failed for JSON property taitotaso due to missing (therefore NULL) value for creator parameter taitotaso which is a non-nullable type",
            )
        }
    }

    @Test
    fun `Arviointipaivat siirretaan oikein`() {
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
                                    tutkintopaiva = LocalDate.of(2025, 9, 12),
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Hyvä,
                                            paivamaara = LocalDate.of(2025, 9, 12),
                                        ),
                                ),
                                VktPuhumisenKoe(
                                    tutkintopaiva = LocalDate.of(2025, 9, 12),
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Hyvä,
                                            paivamaara = LocalDate.of(2025, 9, 12),
                                        ),
                                ),
                                VktPuheenYmmartamisenKoe(
                                    tutkintopaiva = LocalDate.of(2025, 9, 11),
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Tyydyttävä,
                                            paivamaara = LocalDate.of(2025, 9, 11),
                                        ),
                                ),
                                VktTekstinYmmartamisenKoe(
                                    tutkintopaiva = LocalDate.of(2025, 9, 11),
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Hyvä,
                                            paivamaara = LocalDate.of(2025, 9, 11),
                                        ),
                                ),
                                VktKirjoittamisenKoe(
                                    tutkintopaiva = LocalDate.of(2025, 9, 11),
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Hyvä,
                                            paivamaara = LocalDate.of(2025, 9, 11),
                                        ),
                                ),
                                VktPuhumisenKoe(
                                    tutkintopaiva = LocalDate.of(2025, 9, 11),
                                    arviointi =
                                        VktArvionti(
                                            arvosana = Koodisto.VktArvosana.Tyydyttävä,
                                            paivamaara = LocalDate.of(2025, 9, 11),
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

        pts.osasuoritukset.forEach { os ->
            val tutkinto = os as VktKielitaito
            tutkinto.osasuoritukset.forEach { osakoe ->
                osakoe.arviointi.forEach { arviointi ->
                    assertTrue(
                        arviointi.päivä >= tutkinto.alkamispäivä!!,
                        "Osakokeen arviointi ${arviointi.päivä} on aiemmin kuin tutkinnon alkamispäivä ${tutkinto.alkamispäivä}: $tutkinto",
                    )
                    assertTrue(
                        arviointi.päivä >= osakoe.alkamispäivä!!,
                        "Osakokeen arviointi ${arviointi.päivä} on aiemmin kuin osakokeen alkamispäivä ${osakoe.alkamispäivä}: $tutkinto",
                    )
                }
                assertTrue(
                    osakoe.alkamispäivä!! >= tutkinto.alkamispäivä!!,
                    "Osakokeen alkamispäivä ${osakoe.alkamispäivä} on aiemmin kuin tutkinnon alkamispäivä ${tutkinto.alkamispäivä}: $tutkinto",
                )
            }
        }
    }

    @Test
    fun `internalId tuominen tiedonsiirrossa aiheuttaa virheen`() {
        val suoritus = SchemaTests.vktHenkilosuoritus
        putSuoritus(
            suoritus.copy(
                suoritus = suoritus.suoritus.copy(internalId = 10),
            ),
        ) {
            isBadRequest("suoritus.internalId: internalId on sisäinen kenttä, eikä sitä voi asettaa")
        }
    }

    @Test
    fun `koskiSiirtoKasitelty tuominen tiedonsiirrossa aiheuttaa virheen`() {
        val suoritus = SchemaTests.vktHenkilosuoritus
        putSuoritus(
            suoritus.copy(
                suoritus = suoritus.suoritus.copy(koskiSiirtoKasitelty = true),
            ),
        ) {
            isBadRequest(
                "suoritus.koskiSiirtoKasitelty: koskiSiirtoKasitelty on sisäinen kenttä, eikä sitä voi asettaa arvoon true",
            )
        }
    }

    @Test
    fun `koskiOpiskeluoikeusOid tuominen tiedonsiirrossa aiheuttaa virheen`() {
        val suoritus = SchemaTests.vktHenkilosuoritus
        putSuoritus(
            suoritus.copy(
                suoritus =
                    suoritus.suoritus.copy(
                        koskiOpiskeluoikeusOid = Oid.parse("1.2.246.562.15.59238287235").getOrThrow(),
                    ),
            ),
        ) {
            isBadRequest(
                "suoritus.koskiOpiskeluoikeusOid: koskiOpiskeluoikeusOid on sisäinen kenttä, eikä sitä voi asettaa",
            )
        }
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
