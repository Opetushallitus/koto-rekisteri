package fi.oph.kitu.yhteystiedot

import fi.oph.kitu.DBContainerConfiguration
import fi.oph.kitu.mock.generateRandomYkiSuoritusEntity
import fi.oph.kitu.oid.Oid
import fi.oph.kitu.yki.suoritukset.YkiSuoritusRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.testcontainers.postgresql.PostgreSQLContainer
import kotlin.test.Test

@SpringBootTest
@Import(DBContainerConfiguration::class)
class YhteystiedotApiControllerTest {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired private var postgres: PostgreSQLContainer? = null
    private var mockMvc: MockMvc? = null

    @Autowired private lateinit var yhteystiedotService: YhteystiedotService

    @Autowired private lateinit var ykiSuoritukset: YkiSuoritusRepository

    @BeforeEach
    fun setup() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply { springSecurity() }
                .build()
    }

    @Test
    fun `Hakeminen olemassaolevalla oidilla palauttaa yhteystiedot`() {
        // Setup
        val opiskeluoikeusOid = Oid.parse("1.2.246.562.24.20281155246").getOrThrow()
        val suoritus =
            generateRandomYkiSuoritusEntity().copy(
                koskiOpiskeluoikeus = opiskeluoikeusOid,
                koskiSiirtoKasitelty = true,
            )
        ykiSuoritukset.save(suoritus, updateOnConflict = true)

        // Act
        mockMvc!!
            .get("/yhteystiedot/api/opiskeluoikeus/$opiskeluoikeusOid") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isOk() }
                content {
                    json(
                        """
                        {
                            "sukunimi": "${suoritus.sukunimi}",
                            "etunimet": "${suoritus.etunimet}",
                            "katuosoite": "${suoritus.katuosoite}",
                            "postinumero": "${suoritus.postinumero}",
                            "postitoimipaikka": "${suoritus.postitoimipaikka}",
                            "maa": {
                                "koodiarvo": "${suoritus.maa}",
                                "koodistoUri": "maatjavaltiot1"
                            },
                            "email": "${suoritus.email}",
                            "todistuskieli": {
                                "koodiarvo": "${suoritus.todistuskieli!!.kieliKoodistoarvo}",
                                "koodistoUri": "kieli"
                            }
                        }
                        """.trimIndent(),
                    )
                }
            }
    }

    @Test
    fun `Hakeminen muulla oidilla palauttaa yhteystiedot`() {
        val opiskeluoikeusOid = "1.2.246.562.24.00000000000"

        // Act
        mockMvc!!
            .get("/yhteystiedot/api/opiskeluoikeus/$opiskeluoikeusOid") {
                accept = MediaType.APPLICATION_JSON
            }.andExpect {
                status { isNotFound() }
                content {
                    json(
                        """
                        {
                            "request": "$opiskeluoikeusOid",
                            "error": "NOT_FOUND"
                        }
                        """.trimIndent(),
                    )
                }
            }
    }
}
